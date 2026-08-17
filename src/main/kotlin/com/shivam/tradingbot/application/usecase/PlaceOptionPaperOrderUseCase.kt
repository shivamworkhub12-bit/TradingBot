package com.shivam.tradingbot.application.usecase

import com.shivam.tradingbot.application.port.out.OptionPaperFillStorePort
import com.shivam.tradingbot.application.port.out.OptionPaperPortfolioStorePort
import com.shivam.tradingbot.domain.fno.LongOptionRiskManager
import com.shivam.tradingbot.domain.fno.OptionOrderIntent
import com.shivam.tradingbot.domain.fno.OptionPaperFill
import com.shivam.tradingbot.domain.risk.RiskDecision
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

open class PlaceOptionPaperOrderUseCase(
    private val portfolioStore: OptionPaperPortfolioStorePort,
    private val fillStore: OptionPaperFillStorePort,
    private val riskManager: LongOptionRiskManager,
) {
    @Transactional
    open fun execute(order: OptionOrderIntent, asOf: LocalDate): OptionPaperOrderResult {
        val portfolio = portfolioStore.load()
        return when (val decision = riskManager.assess(order, portfolio.availableCash, java.math.BigDecimal.ZERO, asOf)) {
            is RiskDecision.Rejected -> OptionPaperOrderResult.Rejected(decision.reason)
            is RiskDecision.Approved -> {
                val fill = OptionPaperFill(UUID.randomUUID(), order, Instant.now())
                portfolioStore.save(portfolio.applyBuy(order))
                fillStore.save(fill)
                OptionPaperOrderResult.Filled(fill)
            }
        }
    }
}

open class CloseOptionPaperPositionUseCase(
    private val portfolioStore: OptionPaperPortfolioStorePort,
    private val fillStore: OptionPaperFillStorePort,
) {
    @Transactional
    open fun execute(tradingSymbol: String, exitPremium: java.math.BigDecimal): OptionPaperOrderResult {
        val portfolio = portfolioStore.load()
        val position = portfolio.positions[tradingSymbol]
            ?: return OptionPaperOrderResult.Rejected("No open paper position exists for $tradingSymbol")
        val order = OptionOrderIntent(
            position.contract,
            com.shivam.tradingbot.domain.model.OrderSide.SELL,
            position.lots,
            exitPremium,
        )
        val fill = OptionPaperFill(UUID.randomUUID(), order, Instant.now())
        portfolioStore.save(portfolio.applySell(order))
        fillStore.save(fill)
        return OptionPaperOrderResult.Filled(fill)
    }
}

sealed interface OptionPaperOrderResult {
    data class Filled(val fill: OptionPaperFill) : OptionPaperOrderResult
    data class Rejected(val reason: String) : OptionPaperOrderResult
}
