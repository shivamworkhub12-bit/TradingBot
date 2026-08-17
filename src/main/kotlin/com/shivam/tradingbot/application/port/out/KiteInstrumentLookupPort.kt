package com.shivam.tradingbot.application.port.out

fun interface KiteInstrumentLookupPort {
    fun find(symbol: String): KiteInstrument
}

data class KiteInstrument(
    val symbol: String,
    val instrumentToken: Long,
)
