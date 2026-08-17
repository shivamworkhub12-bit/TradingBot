package com.shivam.tradingbot.application.port.out

import com.shivam.tradingbot.domain.model.Candle
import java.time.Instant

fun interface KiteHistoricalDataPort {
    fun load(request: KiteHistoricalDataRequest): List<Candle>
}

data class KiteHistoricalDataRequest(
    val symbol: String,
    val instrumentToken: Long,
    val interval: KiteCandleInterval,
    val from: Instant,
    val to: Instant,
)

enum class KiteCandleInterval(val apiValue: String) {
    MINUTE("minute"),
    THREE_MINUTE("3minute"),
    FIVE_MINUTE("5minute"),
    TEN_MINUTE("10minute"),
    FIFTEEN_MINUTE("15minute"),
    THIRTY_MINUTE("30minute"),
    SIXTY_MINUTE("60minute"),
    DAY("day"),
}
