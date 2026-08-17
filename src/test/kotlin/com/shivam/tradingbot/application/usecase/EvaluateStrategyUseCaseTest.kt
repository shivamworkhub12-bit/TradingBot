package com.shivam.tradingbot.application.usecase

import com.shivam.tradingbot.application.port.out.MarketDataPort
import com.shivam.tradingbot.application.port.out.SignalStorePort
import com.shivam.tradingbot.domain.model.Candle
import com.shivam.tradingbot.domain.model.Signal
import com.shivam.tradingbot.domain.model.SignalAction
import com.shivam.tradingbot.domain.strategy.MovingAverageCrossoverStrategy
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class EvaluateStrategyUseCaseTest {
    @Test
    fun `loads prices evaluates the strategy and persists the signal`() {
        val marketData = RecordingMarketData(candles(10, 10, 10, 13))
        val signalStore = InMemorySignalStore()
        val useCase = EvaluateStrategyUseCase(
            marketData = marketData,
            signalStore = signalStore,
            strategy = MovingAverageCrossoverStrategy(shortWindow = 2, longWindow = 3),
            candleHistorySize = 4,
        )

        val result = useCase.execute("NSE:INFY")

        assertEquals("NSE:INFY" to 4, marketData.lastRequest)
        assertEquals(SignalAction.BUY, result.action)
        assertSame(result, signalStore.signals.single())
    }

    private class RecordingMarketData(private val candles: List<Candle>) : MarketDataPort {
        var lastRequest: Pair<String, Int>? = null

        override fun loadClosedCandles(symbol: String, limit: Int): List<Candle> {
            lastRequest = symbol to limit
            return candles
        }
    }

    private class InMemorySignalStore : SignalStorePort {
        val signals = mutableListOf<Signal>()

        override fun save(signal: Signal) {
            signals += signal
        }
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
