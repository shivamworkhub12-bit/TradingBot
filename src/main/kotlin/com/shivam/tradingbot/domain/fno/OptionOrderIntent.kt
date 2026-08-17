package com.shivam.tradingbot.domain.fno

import com.shivam.tradingbot.domain.model.OrderSide
import java.math.BigDecimal

/** A proposed option order expressed in lots rather than individual units. */
data class OptionOrderIntent(
    val contract: OptionContract,
    val side: OrderSide,
    val lots: Int,
    val expectedPremium: BigDecimal,
) {
    init {
        require(lots > 0) { "lots must be positive" }
        require(expectedPremium > BigDecimal.ZERO) { "expectedPremium must be positive" }
    }

    val quantity: Int get() = lots * contract.lotSize
    val premiumExposure: BigDecimal get() = expectedPremium.multiply(BigDecimal(quantity))
}
