package com.shivam.tradingbot.application.usecase

import com.shivam.tradingbot.domain.model.OrderIntent
import com.shivam.tradingbot.domain.model.PaperPortfolio
import com.shivam.tradingbot.domain.risk.RiskDecision
import com.shivam.tradingbot.domain.risk.RiskManager

/** Application boundary for deciding whether a proposed paper order is allowed. */
class AssessOrderRiskUseCase(
    private val riskManager: RiskManager,
) {
    fun execute(order: OrderIntent, portfolio: PaperPortfolio): RiskDecision =
        riskManager.assess(order, portfolio)
}
