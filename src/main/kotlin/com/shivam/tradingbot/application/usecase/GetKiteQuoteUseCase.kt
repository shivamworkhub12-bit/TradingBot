package com.shivam.tradingbot.application.usecase

import com.shivam.tradingbot.application.port.out.KiteQuote
import com.shivam.tradingbot.application.port.out.KiteQuoteLookupPort

class GetKiteQuoteUseCase(private val quoteLookup: KiteQuoteLookupPort) {
    fun execute(symbol: String): KiteQuote {
        require(symbol.isNotBlank()) { "symbol must not be blank" }
        return quoteLookup.latest(symbol)
    }
}
