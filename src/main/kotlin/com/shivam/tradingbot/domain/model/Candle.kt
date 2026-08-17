package com.shivam.tradingbot.domain.model

import java.math.BigDecimal
import java.time.Instant

/**
 * One completed price bar for an instrument and time frame.
 *
 * A candle belongs to the domain because strategies reason about prices, not about
 * HTTP responses, database rows, or a particular market-data provider.
 */
data class Candle(
    val symbol: String,
    val closedAt: Instant,
    val open: BigDecimal,
    val high: BigDecimal,
    val low: BigDecimal,
    val close: BigDecimal,
    val volume: Long,
) {
    init {
        require(symbol.isNotBlank()) { "symbol must not be blank" }
        require(volume >= 0) { "volume must not be negative" }
        require(low <= high) { "low must not exceed high" }
        require(open in low..high) { "open must be between low and high" }
        require(close in low..high) { "close must be between low and high" }
    }
}
