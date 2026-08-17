package com.shivam.tradingbot.adapter.`in`.web

internal fun validatedLimit(limit: Int): Int {
    require(limit in 1..100) { "limit must be between 1 and 100" }
    return limit
}
