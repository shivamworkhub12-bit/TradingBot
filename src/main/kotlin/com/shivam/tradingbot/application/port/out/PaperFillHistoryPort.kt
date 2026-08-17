package com.shivam.tradingbot.application.port.out

import com.shivam.tradingbot.domain.model.PaperFill

/** Read-only fill history needed by the HTTP/API layer. */
fun interface PaperFillHistoryPort {
    fun latest(limit: Int): List<PaperFill>
}
