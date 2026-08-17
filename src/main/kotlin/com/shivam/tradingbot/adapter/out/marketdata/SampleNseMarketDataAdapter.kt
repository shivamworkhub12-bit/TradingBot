package com.shivam.tradingbot.adapter.out.marketdata

import com.shivam.tradingbot.application.port.out.MarketDataPort
import com.shivam.tradingbot.domain.model.Candle
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Temporary development adapter. It provides deterministic completed candles so
 * we can exercise the application without a broker account or live market data.
 */
class SampleNseMarketDataAdapter : MarketDataPort {
    override fun loadClosedCandles(symbol: String, limit: Int): List<Candle> {
        require(limit > 0) { "limit must be positive" }

        return sampleCloses.takeLast(limit).mapIndexed { index, close ->
            Candle(
                symbol = symbol,
                closedAt = firstCloseTime.plus(index.toLong(), ChronoUnit.DAYS),
                open = close,
                high = close,
                low = close,
                close = close,
                volume = 100_000,
            )
        }
    }

    private companion object {
        val firstCloseTime: Instant = Instant.parse("2026-01-01T09:15:00Z")
        val sampleCloses = listOf("1", "1", "1", "1", "1", "1", "1", "1", "1", "1", "2")
            .map(::BigDecimal)
    }
}
