package com.shivam.tradingbot.domain.strategy

import com.shivam.tradingbot.domain.model.Candle
import com.shivam.tradingbot.domain.model.SignalAction
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class MovingAverageCrossoverStrategyTest {
    private val strategy = MovingAverageCrossoverStrategy(shortWindow = 2, longWindow = 3)

    @Test
    fun `returns hold while there are not enough candles`() {
        val signal = strategy.evaluate(candles(100, 101, 102))

        assertEquals(SignalAction.HOLD, signal.action)
    }

    @Test
    fun `returns buy when short average crosses above long average`() {
        val signal = strategy.evaluate(candles(10, 10, 10, 13))

        assertEquals(SignalAction.BUY, signal.action)
    }

    @Test
    fun `returns sell when short average crosses below long average`() {
        val signal = strategy.evaluate(candles(13, 13, 13, 10))

        assertEquals(SignalAction.SELL, signal.action)
    }

    private fun candles(vararg closes: Int): List<Candle> = closes.mapIndexed { index, close ->
        Candle(
            symbol = "NSE:INFY",
            closedAt = Instant.parse("2026-01-01T00:00:00Z").plusSeconds(index * 86_400L),
            open = BigDecimal(close),
            high = BigDecimal(close),
            low = BigDecimal(close),
            close = BigDecimal(close),
            volume = 1_000,
        )
    }
}
