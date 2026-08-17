package com.shivam.tradingbot.adapter.`in`.web

import com.shivam.tradingbot.application.port.out.KiteInstrument
import com.shivam.tradingbot.application.usecase.FindKiteInstrumentUseCase
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/kite/instruments")
class KiteInstrumentController(
    private val findKiteInstrument: FindKiteInstrumentUseCase,
) {
    @GetMapping("/lookup")
    fun lookup(@RequestParam symbol: String): KiteInstrumentResponse =
        KiteInstrumentResponse.from(findKiteInstrument.execute(symbol))
}

data class KiteInstrumentResponse(
    val symbol: String,
    val instrumentToken: Long,
) {
    companion object {
        fun from(instrument: KiteInstrument) = KiteInstrumentResponse(instrument.symbol, instrument.instrumentToken)
    }
}
