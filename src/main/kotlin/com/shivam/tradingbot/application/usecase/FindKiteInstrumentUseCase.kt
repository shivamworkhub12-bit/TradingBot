package com.shivam.tradingbot.application.usecase

import com.shivam.tradingbot.application.port.out.KiteInstrument
import com.shivam.tradingbot.application.port.out.KiteInstrumentLookupPort

class FindKiteInstrumentUseCase(
    private val kiteInstrumentLookup: KiteInstrumentLookupPort,
) {
    fun execute(symbol: String): KiteInstrument {
        require(symbol.matches(Regex("[A-Z]+:[A-Z0-9 -]+"))) {
            "symbol must use EXCHANGE:TRADINGSYMBOL format, for example NSE:INFY"
        }
        return kiteInstrumentLookup.find(symbol)
    }
}
