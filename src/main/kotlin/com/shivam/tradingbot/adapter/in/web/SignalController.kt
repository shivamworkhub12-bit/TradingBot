package com.shivam.tradingbot.adapter.`in`.web

import com.shivam.tradingbot.application.usecase.EvaluateStrategyUseCase
import com.shivam.tradingbot.domain.model.Signal
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

/** HTTP is an inbound adapter: it translates a request into a use-case call. */
@RestController
@RequestMapping("/signals")
class SignalController(
    private val evaluateStrategy: EvaluateStrategyUseCase,
) {
    @PostMapping("/evaluate")
    @ResponseStatus(HttpStatus.CREATED)
    fun evaluate(@RequestParam symbol: String): SignalResponse =
        SignalResponse.from(evaluateStrategy.execute(symbol))
}

data class SignalResponse(
    val symbol: String,
    val action: String,
    val generatedAt: Instant,
    val reason: String,
) {
    companion object {
        fun from(signal: Signal) = SignalResponse(
            symbol = signal.symbol,
            action = signal.action.name,
            generatedAt = signal.generatedAt,
            reason = signal.reason,
        )
    }
}
