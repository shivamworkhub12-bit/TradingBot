package com.shivam.tradingbot.application.port.out

import java.time.Instant

interface KiteAuthenticationPort {
    fun loginUrl(): String
    fun completeLogin(requestToken: String): KiteSession
}

data class KiteSession(
    val expiresAt: Instant,
)
