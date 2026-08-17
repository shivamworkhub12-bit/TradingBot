package com.shivam.tradingbot.application.usecase

import com.shivam.tradingbot.application.port.out.KiteAuthenticationPort
import com.shivam.tradingbot.application.port.out.KiteSession

class StartKiteLoginUseCase(
    private val kiteAuthentication: KiteAuthenticationPort,
) {
    fun execute(): String = kiteAuthentication.loginUrl()
}

class CompleteKiteLoginUseCase(
    private val kiteAuthentication: KiteAuthenticationPort,
) {
    fun execute(requestToken: String): KiteSession {
        require(requestToken.isNotBlank()) { "requestToken must not be blank" }
        return kiteAuthentication.completeLogin(requestToken)
    }
}
