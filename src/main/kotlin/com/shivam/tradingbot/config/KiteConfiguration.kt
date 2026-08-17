package com.shivam.tradingbot.config

import com.shivam.tradingbot.adapter.out.kite.KiteConnectClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class KiteConfiguration {
    @Bean
    fun kiteConnectClient(
        @Value("\${KITE_API_KEY:}") apiKey: String,
        @Value("\${KITE_API_SECRET:}") apiSecret: String,
    ) = KiteConnectClient(apiKey, apiSecret)
}
