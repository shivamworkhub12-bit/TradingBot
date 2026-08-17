package com.shivam.tradingbot.domain.risk

import com.shivam.tradingbot.domain.model.OrderIntent
import com.shivam.tradingbot.domain.model.PaperPortfolio

fun interface RiskManager {
    fun assess(order: OrderIntent, portfolio: PaperPortfolio): RiskDecision
}
