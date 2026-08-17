package com.shivam.tradingbot.application.port.out

import com.shivam.tradingbot.domain.model.Signal

/** Stores an evaluated signal so it can be audited or displayed later. */
fun interface SignalStorePort {
    fun save(signal: Signal)
}
