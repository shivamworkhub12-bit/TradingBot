package com.shivam.tradingbot.domain.model

import java.time.Instant

/** A strategy's recommendation. It never places an order by itself. */
data class Signal(
    val symbol: String,
    val action: SignalAction,
    val generatedAt: Instant,
    val reason: String,
)

enum class SignalAction {
    BUY,
    SELL,
    HOLD,
}
