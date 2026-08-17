package com.shivam.tradingbot.application.usecase

import com.shivam.tradingbot.application.port.out.PaperBrokerPort
import com.shivam.tradingbot.application.port.out.PaperFillStorePort
import com.shivam.tradingbot.application.port.out.PaperPortfolioStorePort
import com.shivam.tradingbot.domain.model.OrderIntent
import com.shivam.tradingbot.domain.model.PaperFill
import com.shivam.tradingbot.domain.risk.RiskDecision
import com.shivam.tradingbot.domain.risk.RiskManager
import org.springframework.transaction.annotation.Transactional

open class PlacePaperOrderUseCase(
    private val portfolioStore: PaperPortfolioStorePort,
    private val paperBroker: PaperBrokerPort,
    private val fillStore: PaperFillStorePort,
    private val riskManager: RiskManager,
) {
    @Transactional
    open fun execute(order: OrderIntent): PaperOrderResult {
        val portfolio = portfolioStore.load()
        return when (val decision = riskManager.assess(order, portfolio)) {
            is RiskDecision.Rejected -> PaperOrderResult.Rejected(decision.reason)
            is RiskDecision.Approved -> {
                val fill = paperBroker.fill(order)
                portfolioStore.save(portfolio.apply(fill))
                fillStore.save(fill)
                PaperOrderResult.Filled(fill)
            }
        }
    }
}

sealed interface PaperOrderResult {
    data class Filled(val fill: PaperFill) : PaperOrderResult
    data class Rejected(val reason: String) : PaperOrderResult
}
