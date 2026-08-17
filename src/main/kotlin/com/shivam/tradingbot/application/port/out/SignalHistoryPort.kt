package com.shivam.tradingbot.application.port.out

import com.shivam.tradingbot.domain.model.Signal

/** Read-only history needed by the HTTP/API layer. */
fun interface SignalHistoryPort {
    fun latest(limit: Int): List<Signal>
}
