package com.shivam.tradingbot.domain.fno

import java.math.BigDecimal

data class OptionPaperPosition(
    val contract: OptionContract,
    val lots: Int,
    val averagePremium: BigDecimal,
)

data class OptionPaperPortfolio(
    val availableCash: BigDecimal,
    val realizedProfitLoss: BigDecimal = BigDecimal.ZERO,
    val positions: Map<String, OptionPaperPosition> = emptyMap(),
) {
    fun applyBuy(order: OptionOrderIntent): OptionPaperPortfolio {
        require(availableCash >= order.premiumExposure) { "insufficient cash for option premium" }
        require(!positions.containsKey(order.contract.tradingSymbol)) { "an option position already exists for this contract" }
        return copy(
            availableCash = availableCash.subtract(order.premiumExposure),
            positions = positions + (order.contract.tradingSymbol to OptionPaperPosition(order.contract, order.lots, order.expectedPremium)),
        )
    }

    fun applySell(order: OptionOrderIntent): OptionPaperPortfolio {
        val position = requireNotNull(positions[order.contract.tradingSymbol]) { "no open paper position exists for this option" }
        require(order.lots == position.lots) { "paper mode currently closes the full option position only" }
        val proceeds = order.premiumExposure
        val costBasis = position.averagePremium.multiply(BigDecimal(order.quantity))
        return copy(
            availableCash = availableCash.add(proceeds),
            realizedProfitLoss = realizedProfitLoss.add(proceeds.subtract(costBasis)),
            positions = positions - order.contract.tradingSymbol,
        )
    }
}
