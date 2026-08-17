package com.shivam.tradingbot.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

/** The minimum portfolio data needed for the first risk checks. */
data class PaperPortfolio(
    val availableCash: BigDecimal,
    val dailyRealizedProfitLoss: BigDecimal,
    val positions: Map<String, PaperPosition> = emptyMap(),
) {
    init {
        require(availableCash >= BigDecimal.ZERO) { "availableCash must not be negative" }
    }

    fun apply(fill: PaperFill): PaperPortfolio = when (fill.order.side) {
        OrderSide.BUY -> applyBuy(fill)
        OrderSide.SELL -> applySell(fill)
    }

    private fun applyBuy(fill: PaperFill): PaperPortfolio {
        val totalCost = fill.value.add(fill.transactionCost)
        require(availableCash >= totalCost) { "insufficient cash for fill" }
        val existing = positions[fill.order.symbol]
        val position = if (existing == null) {
            PaperPosition(
                fill.order.symbol,
                fill.order.quantity,
                totalCost.divide(BigDecimal(fill.order.quantity), 8, RoundingMode.HALF_UP),
            )
        } else {
            val totalQuantity = existing.quantity + fill.order.quantity
            val totalCost = existing.averagePrice.multiply(BigDecimal(existing.quantity))
                .add(fill.fillPrice.multiply(BigDecimal(fill.order.quantity)))
            PaperPosition(
                fill.order.symbol,
                totalQuantity,
            totalCost.add(existing.averagePrice.multiply(BigDecimal(existing.quantity)))
                .divide(BigDecimal(totalQuantity), 8, RoundingMode.HALF_UP),
            )
        }
        return copy(
            availableCash = availableCash.subtract(totalCost),
            positions = positions + (fill.order.symbol to position),
        )
    }

    private fun applySell(fill: PaperFill): PaperPortfolio {
        val existing = requireNotNull(positions[fill.order.symbol]) { "no position exists for ${fill.order.symbol}" }
        require(existing.quantity >= fill.order.quantity) { "cannot sell more than the open position" }

        val realizedProfitLoss = fill.value.subtract(fill.transactionCost)
            .subtract(existing.averagePrice.multiply(BigDecimal(fill.order.quantity)))
        val remainingQuantity = existing.quantity - fill.order.quantity
        val updatedPositions = if (remainingQuantity == 0) {
            positions - fill.order.symbol
        } else {
            positions + (fill.order.symbol to existing.copy(quantity = remainingQuantity))
        }
        return copy(
            availableCash = availableCash.add(fill.value).subtract(fill.transactionCost),
            dailyRealizedProfitLoss = dailyRealizedProfitLoss.add(realizedProfitLoss),
            positions = updatedPositions,
        )
    }
}
