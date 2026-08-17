package com.shivam.tradingbot.domain.model

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class PaperPortfolioTest {
    @Test
    fun `includes buy and sell transaction costs in realized profit loss`() {
        val buy = fill(OrderSide.BUY, "100", "2")
        val sell = fill(OrderSide.SELL, "110", "3")

        val portfolio = PaperPortfolio(BigDecimal("1000"), BigDecimal.ZERO)
            .apply(buy)
            .apply(sell)

        assertEquals(BigDecimal("5.00000000"), portfolio.dailyRealizedProfitLoss)
        assertEquals(BigDecimal("1005"), portfolio.availableCash)
    }

    private fun fill(side: OrderSide, price: String, cost: String) = PaperFill(
        id = UUID.randomUUID(),
        order = OrderIntent("NSE:INFY", side, 1, BigDecimal(price)),
        fillPrice = BigDecimal(price),
        filledAt = Instant.EPOCH,
        transactionCost = BigDecimal(cost),
    )
}
