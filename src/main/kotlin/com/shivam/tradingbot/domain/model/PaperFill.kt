package com.shivam.tradingbot.domain.model

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/** A simulated execution produced by a paper broker. */
data class PaperFill(
    val id: UUID,
    val order: OrderIntent,
    val fillPrice: BigDecimal,
    val filledAt: Instant,
    val transactionCost: BigDecimal = BigDecimal.ZERO,
) {
    init {
        require(fillPrice > BigDecimal.ZERO) { "fillPrice must be positive" }
        require(transactionCost >= BigDecimal.ZERO) { "transactionCost must not be negative" }
    }

    val value: BigDecimal
        get() = fillPrice.multiply(BigDecimal(order.quantity))
}
