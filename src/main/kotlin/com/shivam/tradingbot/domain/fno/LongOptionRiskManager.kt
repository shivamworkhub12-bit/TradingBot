package com.shivam.tradingbot.domain.fno

import com.shivam.tradingbot.domain.model.OrderSide
import com.shivam.tradingbot.domain.risk.RiskDecision
import java.math.BigDecimal
import java.time.LocalDate

/**
 * First F&O safety policy: buying an option has a known maximum loss (the premium).
 * Selling options is deliberately disabled until we implement margin and hedge checks.
 */
class LongOptionRiskManager(
    private val maximumPremiumAtRiskPerTrade: BigDecimal = BigDecimal("10000"),
    private val maximumDailyLoss: BigDecimal = BigDecimal("500"),
) {
    fun assess(
        order: OptionOrderIntent,
        availableCash: BigDecimal,
        dailyRealizedProfitLoss: BigDecimal,
        asOf: LocalDate,
    ): RiskDecision = when {
        order.side == OrderSide.SELL -> RiskDecision.Rejected("Naked option selling is disabled in paper mode")
        !order.contract.expiry.isAfter(asOf) -> RiskDecision.Rejected("Option contract is expired or expires today")
        dailyRealizedProfitLoss <= maximumDailyLoss.negate() ->
            RiskDecision.Rejected("Daily loss limit of $maximumDailyLoss has been reached")
        order.premiumExposure > maximumPremiumAtRiskPerTrade ->
            RiskDecision.Rejected("Premium at risk ${order.premiumExposure} exceeds limit $maximumPremiumAtRiskPerTrade")
        order.premiumExposure > availableCash ->
            RiskDecision.Rejected("Premium at risk ${order.premiumExposure} exceeds available cash $availableCash")
        else -> RiskDecision.Approved("Long option premium risk is within paper-trading limits")
    }
}
