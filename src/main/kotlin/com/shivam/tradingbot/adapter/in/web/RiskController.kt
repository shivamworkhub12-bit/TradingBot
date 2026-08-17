package com.shivam.tradingbot.adapter.`in`.web

import com.shivam.tradingbot.application.usecase.AssessOrderRiskUseCase
import com.shivam.tradingbot.domain.model.OrderIntent
import com.shivam.tradingbot.domain.model.OrderSide
import com.shivam.tradingbot.domain.model.PaperPortfolio
import com.shivam.tradingbot.domain.risk.RiskDecision
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

@RestController
@RequestMapping("/risk")
class RiskController(
    private val assessOrderRisk: AssessOrderRiskUseCase,
) {
    @PostMapping("/assess")
    fun assess(
        @RequestParam symbol: String,
        @RequestParam side: OrderSide,
        @RequestParam quantity: Int,
        @RequestParam expectedPrice: BigDecimal,
        @RequestParam availableCash: BigDecimal,
        @RequestParam dailyRealizedProfitLoss: BigDecimal = BigDecimal.ZERO,
    ): RiskResponse {
        val decision = assessOrderRisk.execute(
            order = OrderIntent(symbol, side, quantity, expectedPrice),
            portfolio = PaperPortfolio(availableCash, dailyRealizedProfitLoss),
        )
        return RiskResponse.from(decision)
    }
}

data class RiskResponse(
    val approved: Boolean,
    val reason: String,
) {
    companion object {
        fun from(decision: RiskDecision) = when (decision) {
            is RiskDecision.Approved -> RiskResponse(approved = true, reason = decision.reason)
            is RiskDecision.Rejected -> RiskResponse(approved = false, reason = decision.reason)
        }
    }
}
