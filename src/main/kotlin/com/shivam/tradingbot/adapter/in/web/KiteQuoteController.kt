package com.shivam.tradingbot.adapter.`in`.web

import com.shivam.tradingbot.application.port.out.KiteQuote
import com.shivam.tradingbot.application.usecase.GetKiteQuoteUseCase
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

/** Read-only latest-price endpoint backed by Kite's LTP quote API. */
@RestController
@RequestMapping("/kite/quotes")
class KiteQuoteController(private val getKiteQuote: GetKiteQuoteUseCase) {
    @GetMapping("/ltp")
    fun latest(@RequestParam symbol: String): KiteQuoteResponse = KiteQuoteResponse.from(getKiteQuote.execute(symbol))
}

data class KiteQuoteResponse(
    val symbol: String,
    val instrumentToken: Long,
    val lastPrice: BigDecimal,
) {
    companion object {
        fun from(quote: KiteQuote) = KiteQuoteResponse(quote.symbol, quote.instrumentToken, quote.lastPrice)
    }
}
