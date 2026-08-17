package com.shivam.tradingbot.domain.model

import java.math.BigDecimal

data class PaperPosition(
    val symbol: String,
    val quantity: Int,
    val averagePrice: BigDecimal,
) {
    init {
        require(symbol.isNotBlank()) { "symbol must not be blank" }
        require(quantity > 0) { "quantity must be positive" }
        require(averagePrice > BigDecimal.ZERO) { "averagePrice must be positive" }
    }
}
