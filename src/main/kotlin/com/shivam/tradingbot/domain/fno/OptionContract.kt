package com.shivam.tradingbot.domain.fno

import java.math.BigDecimal
import java.time.LocalDate

/** The immutable terms of one NFO index option contract. */
data class OptionContract(
    val underlying: IndexUnderlying,
    val expiry: LocalDate,
    val strike: BigDecimal,
    val optionType: OptionType,
    val lotSize: Int,
    val tradingSymbol: String,
) {
    init {
        require(strike > BigDecimal.ZERO) { "strike must be positive" }
        require(lotSize > 0) { "lotSize must be positive" }
        require(tradingSymbol.isNotBlank()) { "tradingSymbol must not be blank" }
    }
}

enum class IndexUnderlying { NIFTY, BANKNIFTY }
enum class OptionType { CE, PE }
