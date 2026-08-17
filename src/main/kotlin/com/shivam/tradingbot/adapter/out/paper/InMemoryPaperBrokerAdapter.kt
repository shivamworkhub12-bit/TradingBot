package com.shivam.tradingbot.adapter.out.paper

import com.shivam.tradingbot.application.port.out.PaperBrokerPort
import com.shivam.tradingbot.domain.model.OrderIntent
import com.shivam.tradingbot.domain.model.PaperFill
import java.time.Clock
import java.util.UUID

/** Fills every paper order immediately at its expected price. */
class InMemoryPaperBrokerAdapter(
    private val clock: Clock = Clock.systemUTC(),
) : PaperBrokerPort {
    override fun fill(order: OrderIntent) = PaperFill(
        id = UUID.randomUUID(),
        order = order,
        fillPrice = order.expectedPrice,
        filledAt = clock.instant(),
    )
}
