package com.shivam.tradingbot.adapter.`in`.web

import com.shivam.tradingbot.application.port.out.KiteCandleInterval
import com.shivam.tradingbot.application.port.out.KiteHistoricalDataRequest
import com.shivam.tradingbot.application.usecase.FetchKiteHistoricalCandlesUseCase
import com.shivam.tradingbot.domain.model.Candle
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.OffsetDateTime

@RestController
@RequestMapping("/kite/historical-candles")
class KiteHistoricalCandleController(
    private val fetchKiteHistoricalCandles: FetchKiteHistoricalCandlesUseCase,
) {
    @GetMapping
    fun get(
        @RequestParam symbol: String,
        @RequestParam instrumentToken: Long,
        @RequestParam interval: KiteCandleInterval,
        // A '+' in a query parameter is conventionally decoded as a space.
        // Accept the raw Postman value as text, then restore that offset sign.
        @RequestParam from: String,
        @RequestParam to: String,
    ): List<CandleResponse> = fetchKiteHistoricalCandles.execute(
        KiteHistoricalDataRequest(symbol, instrumentToken, interval, parseOffset(from).toInstant(), parseOffset(to).toInstant()),
    ).map(CandleResponse::from)

    private fun parseOffset(value: String): OffsetDateTime =
        OffsetDateTime.parse(value.replace(' ', '+'))
}

data class CandleResponse(
    val symbol: String,
    val closedAt: String,
    val open: BigDecimal,
    val high: BigDecimal,
    val low: BigDecimal,
    val close: BigDecimal,
    val volume: Long,
) {
    companion object {
        fun from(candle: Candle) = CandleResponse(
            symbol = candle.symbol,
            closedAt = candle.closedAt.toString(),
            open = candle.open,
            high = candle.high,
            low = candle.low,
            close = candle.close,
            volume = candle.volume,
        )
    }
}
