package com.shivam.tradingbot.domain.risk

import com.shivam.tradingbot.domain.model.OrderIntent
import com.shivam.tradingbot.domain.model.OrderSide
import com.shivam.tradingbot.domain.model.PaperPortfolio
import java.math.BigDecimal

/**
 * A deliberately small, deterministic policy for paper trading.
 *
 * A SELL is allowed here because it will later be used to close an existing
 * position. Position ownership is a separate portfolio concern we will add next.
 */
class FixedLimitRiskManager(
    private val maximumOrderValue: BigDecimal,
    private val maximumDailyLoss: BigDecimal,
) : RiskManager {
    init {
        require(maximumOrderValue > BigDecimal.ZERO) { "maximumOrderValue must be positive" }
        require(maximumDailyLoss > BigDecimal.ZERO) { "maximumDailyLoss must be positive" }
    }

    override fun assess(order: OrderIntent, portfolio: PaperPortfolio): RiskDecision = when {
        portfolio.dailyRealizedProfitLoss <= maximumDailyLoss.negate() ->
            RiskDecision.Rejected("Daily loss limit of $maximumDailyLoss has been reached")
        order.side == OrderSide.SELL && (portfolio.positions[order.symbol]?.quantity ?: 0) < order.quantity ->
            RiskDecision.Rejected("Cannot sell more shares than the open position")
        order.side == OrderSide.BUY && order.expectedValue > maximumOrderValue ->
            RiskDecision.Rejected("Order value ${order.expectedValue} exceeds limit $maximumOrderValue")
        order.side == OrderSide.BUY && order.expectedValue > portfolio.availableCash ->
            RiskDecision.Rejected("Order value ${order.expectedValue} exceeds available cash ${portfolio.availableCash}")
        else -> RiskDecision.Approved("Order is within paper-trading risk limits")
    }
}
