package com.shivam.tradingbot.adapter.`in`.web

import com.shivam.tradingbot.application.port.out.OptionPaperPortfolioStorePort
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

@RestController
@RequestMapping("/fno/paper-portfolio")
class OptionPaperPortfolioController(private val portfolioStore: OptionPaperPortfolioStorePort) {
    @GetMapping
    fun get(): OptionPaperPortfolioResponse {
        val portfolio = portfolioStore.load()
        return OptionPaperPortfolioResponse(
            portfolio.availableCash,
            portfolio.realizedProfitLoss,
            portfolio.positions.values.map { p -> OptionPaperPositionResponse(
                p.contract.underlying.name, p.contract.expiry.toString(), p.contract.strike, p.contract.optionType.name,
                p.contract.lotSize, p.contract.tradingSymbol, p.lots, p.averagePremium,
            ) },
        )
    }
}

data class OptionPaperPortfolioResponse(val availableCash: BigDecimal, val realizedProfitLoss: BigDecimal, val openPositions: List<OptionPaperPositionResponse>)
data class OptionPaperPositionResponse(
    val underlying: String, val expiry: String, val strike: BigDecimal, val optionType: String,
    val lotSize: Int, val tradingSymbol: String, val lots: Int, val averagePremium: BigDecimal,
)
