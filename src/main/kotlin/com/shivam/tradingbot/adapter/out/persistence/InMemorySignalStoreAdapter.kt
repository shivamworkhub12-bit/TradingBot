package com.shivam.tradingbot.adapter.out.persistence

import com.shivam.tradingbot.application.port.out.SignalStorePort
import com.shivam.tradingbot.application.port.out.SignalHistoryPort
import com.shivam.tradingbot.domain.model.Signal
import java.util.concurrent.CopyOnWriteArrayList

/** Development-only storage. Replace with a database adapter later. */
class InMemorySignalStoreAdapter : SignalStorePort, SignalHistoryPort {
    private val signals = CopyOnWriteArrayList<Signal>()

    override fun save(signal: Signal) {
        signals += signal
    }

    fun all(): List<Signal> = signals.toList()

    override fun latest(limit: Int): List<Signal> = signals.takeLast(limit).reversed()
}
