package com.shivam.tradingbot.domain.cost

import com.shivam.tradingbot.domain.model.OrderIntent
import com.shivam.tradingbot.domain.model.OrderSide
import java.math.BigDecimal

/** Moves simulated execution prices adversely to model bid/ask spread and market impact. */
interface SlippageModel {
    val basisPoints: Int
    fun apply(order: OrderIntent, marketPrice: BigDecimal): BigDecimal
}

object NoSlippage : SlippageModel {
    override val basisPoints: Int = 0
    override fun apply(order: OrderIntent, marketPrice: BigDecimal): BigDecimal = marketPrice
}

class FixedBpsSlippage(override val basisPoints: Int) : SlippageModel {
    init {
        require(basisPoints >= 0) { "basisPoints must not be negative" }
    }

    override fun apply(order: OrderIntent, marketPrice: BigDecimal): BigDecimal {
        val rate = BigDecimal(basisPoints).movePointLeft(4)
        return when (order.side) {
            OrderSide.BUY -> marketPrice.multiply(BigDecimal.ONE.add(rate))
            OrderSide.SELL -> marketPrice.multiply(BigDecimal.ONE.subtract(rate))
        }
    }
}
