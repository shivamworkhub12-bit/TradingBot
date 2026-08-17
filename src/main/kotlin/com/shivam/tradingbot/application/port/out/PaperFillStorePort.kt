package com.shivam.tradingbot.application.port.out

import com.shivam.tradingbot.domain.model.PaperFill

fun interface PaperFillStorePort {
    fun save(fill: PaperFill)
}
