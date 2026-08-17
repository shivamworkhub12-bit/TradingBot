package com.shivam.tradingbot.adapter.`in`.web

import com.shivam.tradingbot.application.port.out.SignalHistoryPort
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/signals")
class SignalHistoryController(
    private val signalHistory: SignalHistoryPort,
) {
    @GetMapping
    fun latest(@RequestParam(defaultValue = "20") limit: Int): List<SignalResponse> =
        signalHistory.latest(validatedLimit(limit)).map(SignalResponse::from)
}
