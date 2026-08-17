package com.shivam.tradingbot.domain.strategy

import com.shivam.tradingbot.domain.model.Candle
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

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

    private fun candles(closes: List<Int>): List<Candle> = closes.mapIndexed { index, close ->
        val price = BigDecimal(close)
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
