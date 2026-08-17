package com.shivam.tradingbot.application.usecase

import com.shivam.tradingbot.application.port.out.KiteQuoteLookupPort
import com.shivam.tradingbot.application.port.out.OptionPaperPortfolioStorePort
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

class GetOptionPaperPortfolioMtmUseCase(
    private val portfolioStore: OptionPaperPortfolioStorePort,
    private val quoteLookup: KiteQuoteLookupPort,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(): OptionPaperPortfolioMtm {
        val portfolio = portfolioStore.load()
        val today = clock.instant().atZone(ZoneId.of("Asia/Kolkata")).toLocalDate()
        val positions = portfolio.positions.values.map { position ->
            val quote = quoteLookup.latest("NFO:${position.contract.tradingSymbol}")
            val quantity = position.lots * position.contract.lotSize
            val costBasis = position.averagePremium.multiply(BigDecimal(quantity))
            val marketValue = quote.lastPrice.multiply(BigDecimal(quantity))
            OptionPaperPositionMtm(
                tradingSymbol = position.contract.tradingSymbol,
                quantity = quantity,
                averagePremium = position.averagePremium,
                lastPremium = quote.lastPrice,
                costBasis = costBasis,
                marketValue = marketValue,
                unrealizedProfitLoss = marketValue.subtract(costBasis),
                expiryStatus = when {
                    position.contract.expiry.isBefore(today) -> "EXPIRED"
                    position.contract.expiry == today -> "EXPIRES_TODAY"
                    else -> "ACTIVE"
                },
            )
        }
        return OptionPaperPortfolioMtm(
            availableCash = portfolio.availableCash,
            optionMarketValue = positions.fold(BigDecimal.ZERO) { total, position -> total.add(position.marketValue) },
            unrealizedProfitLoss = positions.fold(BigDecimal.ZERO) { total, position -> total.add(position.unrealizedProfitLoss) },
            positions = positions,
        )
    }
}

data class OptionPaperPortfolioMtm(
    val availableCash: BigDecimal,
    val optionMarketValue: BigDecimal,
    val unrealizedProfitLoss: BigDecimal,
    val positions: List<OptionPaperPositionMtm>,
)

data class OptionPaperPositionMtm(
    val tradingSymbol: String,
    val quantity: Int,
    val averagePremium: BigDecimal,
    val lastPremium: BigDecimal,
    val costBasis: BigDecimal,
    val marketValue: BigDecimal,
    val unrealizedProfitLoss: BigDecimal,
    val expiryStatus: String,
)
