package com.shivam.tradingbot.domain.model

import java.math.BigDecimal

/** A proposed paper order. It is not an executed trade. */
data class OrderIntent(
    val symbol: String,
    val side: OrderSide,
    val quantity: Int,
    val expectedPrice: BigDecimal,
) {
    init {
        require(symbol.isNotBlank()) { "symbol must not be blank" }
        require(quantity > 0) { "quantity must be positive" }
        require(expectedPrice > BigDecimal.ZERO) { "expectedPrice must be positive" }
    }

    val expectedValue: BigDecimal
        get() = expectedPrice.multiply(BigDecimal(quantity))
}

enum class OrderSide {
    BUY,
    SELL,
}
