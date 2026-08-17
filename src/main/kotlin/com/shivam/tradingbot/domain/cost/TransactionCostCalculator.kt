package com.shivam.tradingbot.domain.cost

import com.shivam.tradingbot.domain.model.OrderIntent
import com.shivam.tradingbot.domain.model.OrderSide
import java.math.BigDecimal
import java.math.RoundingMode

/** Calculates visible estimated charges for a simulated fill. */
fun interface TransactionCostCalculator {
    fun estimate(order: OrderIntent, fillPrice: BigDecimal): BigDecimal
}

object NoTransactionCosts : TransactionCostCalculator {
    override fun estimate(order: OrderIntent, fillPrice: BigDecimal): BigDecimal = BigDecimal.ZERO
}

/**
 * Estimated NSE equity-delivery costs for an individual Zerodha account.
 * Rates are deliberately isolated here because broker and statutory charges change.
 */
object NseEquityDeliveryCostCalculator : TransactionCostCalculator {
    override fun estimate(order: OrderIntent, fillPrice: BigDecimal): BigDecimal {
        val turnover = fillPrice.multiply(BigDecimal(order.quantity))
        val stt = turnover.multiply(BigDecimal("0.001"))
        val exchangeCharge = turnover.multiply(BigDecimal("0.0000307"))
        val sebiCharge = turnover.multiply(BigDecimal("0.000001"))
        val gst = exchangeCharge.add(sebiCharge).multiply(BigDecimal("0.18"))
        val stampDuty = if (order.side == OrderSide.BUY) turnover.multiply(BigDecimal("0.00015")) else BigDecimal.ZERO
        val depositoryParticipantCharge = if (order.side == OrderSide.SELL) BigDecimal("15.34") else BigDecimal.ZERO

        return stt.add(exchangeCharge).add(sebiCharge).add(gst).add(stampDuty)
            .add(depositoryParticipantCharge)
            .setScale(2, RoundingMode.HALF_UP)
    }
}
