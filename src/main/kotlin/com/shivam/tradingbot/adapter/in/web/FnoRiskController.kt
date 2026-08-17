package com.shivam.tradingbot.adapter.`in`.web

import com.shivam.tradingbot.domain.fno.IndexUnderlying
import com.shivam.tradingbot.domain.fno.LongOptionRiskManager
import com.shivam.tradingbot.domain.fno.OptionContract
import com.shivam.tradingbot.domain.fno.OptionOrderIntent
import com.shivam.tradingbot.domain.fno.OptionType
import com.shivam.tradingbot.domain.model.OrderSide
import com.shivam.tradingbot.domain.risk.RiskDecision
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.LocalDate

/** Validates a NIFTY or BANKNIFTY long-option paper trade. No order is placed. */
@RestController
@RequestMapping("/fno/risk")
class FnoRiskController(
    private val longOptionRiskManager: LongOptionRiskManager,
) {
    @PostMapping("/assess-option")
    fun assess(
        @RequestParam underlying: IndexUnderlying,
        @RequestParam expiry: LocalDate,
        @RequestParam strike: BigDecimal,
        @RequestParam optionType: OptionType,
        @RequestParam lotSize: Int,
        @RequestParam tradingSymbol: String,
        @RequestParam side: OrderSide,
        @RequestParam lots: Int,
        @RequestParam expectedPremium: BigDecimal,
        @RequestParam availableCash: BigDecimal,
        @RequestParam(defaultValue = "0") dailyRealizedProfitLoss: BigDecimal,
        @RequestParam asOf: LocalDate,
    ): FnoRiskResponse {
        val order = OptionOrderIntent(
            OptionContract(underlying, expiry, strike, optionType, lotSize, tradingSymbol),
            side,
            lots,
            expectedPremium,
        )
        val decision = longOptionRiskManager.assess(order, availableCash, dailyRealizedProfitLoss, asOf)
        return FnoRiskResponse(
            approved = decision is RiskDecision.Approved,
            reason = when (decision) {
                is RiskDecision.Approved -> decision.reason
                is RiskDecision.Rejected -> decision.reason
            },
            quantity = order.quantity,
            premiumAtRisk = order.premiumExposure,
            maximumLoss = if (side == OrderSide.BUY) order.premiumExposure else null,
        )
    }
}

data class FnoRiskResponse(
    val approved: Boolean,
    val reason: String,
    val quantity: Int,
    val premiumAtRisk: BigDecimal,
    val maximumLoss: BigDecimal?,
)
