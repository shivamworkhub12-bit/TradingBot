package com.shivam.tradingbot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableScheduling
class TradingBotApplication

fun main(args: Array<String>) {
    runApplication<TradingBotApplication>(*args)
}
