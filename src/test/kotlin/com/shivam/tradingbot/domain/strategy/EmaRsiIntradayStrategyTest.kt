package com.shivam.tradingbot.domain.strategy

import com.shivam.tradingbot.domain.model.Candle
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EmaRsiIntradayStrategyTest {
    @Test
    fun `returns neutral without enough completed candles`() {
        val decision = EmaRsiIntradayStrategy().evaluate(candles(List(22) { 100 }))

        assertEquals(IntradayDirection.NEUTRAL, decision.direction)
    }

    @Test
    fun `returns bullish when fast EMA crosses above slow EMA and RSI is allowed`() {
        val strategy = EmaRsiIntradayStrategy(bullishRsiRange = BigDecimal.ZERO..BigDecimal("100"))

        val decision = strategy.evaluate(candles(List(23) { 100 } + 110))

        assertEquals(IntradayDirection.BULLISH, decision.direction)
    }

    @Test
    fun `returns bearish when fast EMA crosses below slow EMA and RSI is allowed`() {
        val strategy = EmaRsiIntradayStrategy(bearishRsiRange = BigDecimal.ZERO..BigDecimal("100"))

        val decision = strategy.evaluate(candles(List(23) { 100 } + 90))

        assertEquals(IntradayDirection.BEARISH, decision.direction)
    }

    @Test
    fun `RSI filter rejects an overbought bullish crossover`() {
        val decision = EmaRsiIntradayStrategy().evaluate(candles(List(23) { 100 } + 110))

        assertEquals(IntradayDirection.NEUTRAL, decision.direction)
        assertEquals(BigDecimal("100"), decision.rsi)
    }

    @Test
    fun `rejects a crossover whose EMA separation is too weak`() {
        val strategy = EmaRsiIntradayStrategy(bullishRsiRange = BigDecimal.ZERO..BigDecimal("100"))

        val decision = strategy.evaluate(decimalCandles(List(23) { BigDecimal("100") } + BigDecimal("100.001")))

        assertEquals(IntradayDirection.NEUTRAL, decision.direction)
        assertTrue(decision.reason.contains("separation="))
    }

    @Test
    fun `confirms a strong crossover on the following candle`() {
        val strategy = EmaRsiIntradayStrategy(bullishRsiRange = BigDecimal.ZERO..BigDecimal("100"))

        val decision = strategy.evaluate(candles(List(23) { 100 } + listOf(110, 112)))

        assertEquals(IntradayDirection.BULLISH, decision.direction)
    }

    @Test
    fun `allows a breakout up to eight candles after its confirming crossover`() {
        val strategy = EmaRsiIntradayStrategy(
            bullishRsiRange = BigDecimal.ZERO..BigDecimal("100"),
            confirmationCandles = 8,
        )

        val decision = strategy.evaluate(candles(List(23) { 100 } + List(6) { 110 } + 112))

        assertEquals(IntradayDirection.BULLISH, decision.direction)
        assertTrue(decision.reason.contains("crossover=RECENT_BULLISH"))
    }

    @Test
    fun `rejects a breakout after the eight candle confirmation window expires`() {
        val strategy = EmaRsiIntradayStrategy(
            bullishRsiRange = BigDecimal.ZERO..BigDecimal("100"),
            confirmationCandles = 8,
        )

        val decision = strategy.evaluate(candles(List(23) { 100 } + List(9) { 110 } + 112))

        assertEquals(IntradayDirection.NEUTRAL, decision.direction)
        assertTrue(decision.reason.contains("crossover=NONE"))
        assertTrue(decision.reason.contains("breakout=ABOVE_20_HIGH"))
    }

    @Test
    fun `rejects momentum without a fresh price breakout`() {
        val strategy = EmaRsiIntradayStrategy(bullishRsiRange = BigDecimal.ZERO..BigDecimal("100"))

        val decision = strategy.evaluate(candles(List(23) { 100 } + listOf(110, 109)))

        assertEquals(IntradayDirection.NEUTRAL, decision.direction)
        assertTrue(decision.reason.contains("breakout=NONE"))
    }

    @Test
    fun `records each decision gate for paper tuning`() {
        val strategy = EmaRsiIntradayStrategy(bullishRsiRange = BigDecimal.ZERO..BigDecimal("100"))

        val decision = strategy.evaluate(candles(List(23) { 100 } + 110))

        assertTrue(decision.reason.contains("trend=BULLISH"))
        assertTrue(decision.reason.contains("crossover=RECENT_BULLISH"))
        assertTrue(decision.reason.contains("separation="))
        assertTrue(decision.reason.contains("RSI(14)=100.00(PASS)"))
        assertTrue(decision.reason.contains("breakout=ABOVE_20_HIGH"))
        assertTrue(decision.reason.contains("ATR(14)="))
    }

    @Test
    fun `rejects a breakout when the ATR activity threshold is not met`() {
        val strategy = EmaRsiIntradayStrategy(
            bullishRsiRange = BigDecimal.ZERO..BigDecimal("100"),
            minimumAtrBasisPoints = BigDecimal("1000"),
        )

        val decision = strategy.evaluate(candles(List(23) { 100 } + 110))

        assertEquals(IntradayDirection.NEUTRAL, decision.direction)
        assertTrue(decision.reason.contains("ATR(14)="))
    }

    private fun candles(closes: List<Int>): List<Candle> = closes.map { BigDecimal(it) }.let(::decimalCandles)

    private fun decimalCandles(closes: List<BigDecimal>): List<Candle> = closes.mapIndexed { index, price ->
        Candle(
            symbol = "NSE:NIFTY 50",
            closedAt = Instant.parse("2026-08-17T03:45:00Z").plusSeconds(index * 300L),
            open = price,
            high = price,
            low = price,
            close = price,
            volume = 1_000,
        )
    }
}
