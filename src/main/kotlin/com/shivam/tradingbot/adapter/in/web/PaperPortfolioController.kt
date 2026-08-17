package com.shivam.tradingbot.adapter.`in`.web

import com.shivam.tradingbot.application.port.out.PaperPortfolioStorePort
import com.shivam.tradingbot.domain.model.PaperPortfolio
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

/** Read-only HTTP view of the current paper account. */
@RestController
@RequestMapping("/paper-portfolio")
class PaperPortfolioController(
    private val portfolioStore: PaperPortfolioStorePort,
) {
    @GetMapping
    fun get(): PaperPortfolioResponse = PaperPortfolioResponse.from(portfolioStore.load())
}

data class PaperPortfolioResponse(
    val availableCash: BigDecimal,
    val dailyRealizedProfitLoss: BigDecimal,
    val positions: List<PaperPositionResponse>,
) {
    companion object {
        fun from(portfolio: PaperPortfolio) = PaperPortfolioResponse(
            availableCash = portfolio.availableCash,
            dailyRealizedProfitLoss = portfolio.dailyRealizedProfitLoss,
            positions = portfolio.positions.values.sortedBy { it.symbol }.map {
                PaperPositionResponse(it.symbol, it.quantity, it.averagePrice)
            },
        )
    }
}

data class PaperPositionResponse(
    val symbol: String,
    val quantity: Int,
    val averagePrice: BigDecimal,
)
