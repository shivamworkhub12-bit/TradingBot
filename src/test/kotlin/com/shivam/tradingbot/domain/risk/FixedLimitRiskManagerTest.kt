package com.shivam.tradingbot.domain.risk

import com.shivam.tradingbot.domain.model.OrderIntent
import com.shivam.tradingbot.domain.model.OrderSide
import com.shivam.tradingbot.domain.model.PaperPortfolio
import com.shivam.tradingbot.domain.model.PaperPosition
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertIs

class FixedLimitRiskManagerTest {
    private val riskManager = FixedLimitRiskManager(
        maximumOrderValue = BigDecimal("10000"),
        maximumDailyLoss = BigDecimal("500"),
    )

    @Test
    fun `approves an affordable order within configured limits`() {
        val decision = riskManager.assess(order(value = "5000"), portfolio(cash = "10000", pnl = "0"))

        assertIs<RiskDecision.Approved>(decision)
    }

    @Test
    fun `rejects an order above the maximum order value`() {
        val decision = riskManager.assess(order(value = "10001"), portfolio(cash = "20000", pnl = "0"))

        assertIs<RiskDecision.Rejected>(decision)
    }

    @Test
    fun `rejects a buy order when cash is insufficient`() {
        val decision = riskManager.assess(order(value = "5000"), portfolio(cash = "4999", pnl = "0"))

        assertIs<RiskDecision.Rejected>(decision)
    }

    @Test
    fun `rejects an order once the daily loss limit has been reached`() {
        val decision = riskManager.assess(order(value = "100"), portfolio(cash = "10000", pnl = "-500"))

        assertIs<RiskDecision.Rejected>(decision)
    }

    @Test
    fun `rejects a sell order without a sufficient open position`() {
        val order = OrderIntent("NSE:INFY", OrderSide.SELL, 2, BigDecimal("100"))
        val portfolio = PaperPortfolio(
            availableCash = BigDecimal("10000"),
            dailyRealizedProfitLoss = BigDecimal.ZERO,
            positions = mapOf("NSE:INFY" to PaperPosition("NSE:INFY", 1, BigDecimal("90"))),
        )

        assertIs<RiskDecision.Rejected>(riskManager.assess(order, portfolio))
    }

    private fun order(value: String) = OrderIntent(
        symbol = "NSE:INFY",
        side = OrderSide.BUY,
        quantity = 1,
        expectedPrice = BigDecimal(value),
    )

    private fun portfolio(cash: String, pnl: String) = PaperPortfolio(
        availableCash = BigDecimal(cash),
        dailyRealizedProfitLoss = BigDecimal(pnl),
    )
}
