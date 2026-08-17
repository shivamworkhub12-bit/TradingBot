package com.shivam.tradingbot.application.port.out

import com.shivam.tradingbot.domain.model.OrderIntent
import com.shivam.tradingbot.domain.model.PaperFill

/** Outbound port for a simulated execution venue. */
fun interface PaperBrokerPort {
    fun fill(order: OrderIntent): PaperFill
}
