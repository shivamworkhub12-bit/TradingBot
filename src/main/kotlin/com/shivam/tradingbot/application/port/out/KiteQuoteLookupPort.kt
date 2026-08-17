package com.shivam.tradingbot.application.port.out

import java.math.BigDecimal

fun interface KiteQuoteLookupPort {
    fun latest(symbol: String): KiteQuote
}

data class KiteQuote(
    val symbol: String,
    val instrumentToken: Long,
    val lastPrice: BigDecimal,
)
