package com.shivam.tradingbot.adapter.`in`.web

import com.shivam.tradingbot.application.usecase.CompleteKiteLoginUseCase
import com.shivam.tradingbot.application.usecase.StartKiteLoginUseCase
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.view.RedirectView
import java.time.Instant

@RestController
@RequestMapping("/auth/kite")
class KiteAuthenticationController(
    private val startKiteLogin: StartKiteLoginUseCase,
    private val completeKiteLogin: CompleteKiteLoginUseCase,
) {
    @GetMapping("/login")
    fun login(): RedirectView = RedirectView(startKiteLogin.execute())

    @GetMapping("/callback")
    @ResponseStatus(HttpStatus.OK)
    fun callback(
        @RequestParam("request_token") requestToken: String,
        @RequestParam status: String,
    ): KiteLoginResponse {
        require(status.equals("success", ignoreCase = true)) { "Kite login did not succeed" }
        val session = completeKiteLogin.execute(requestToken)
        return KiteLoginResponse("AUTHENTICATED", session.expiresAt)
    }
}

data class KiteLoginResponse(
    val status: String,
    val accessTokenExpiresAt: Instant,
)
