package com.shivam.tradingbot.application.port.out

import com.shivam.tradingbot.domain.strategy.StrategyDecisionRecord

fun interface StrategyDecisionStorePort {
    fun save(decision: StrategyDecisionRecord)
}

fun interface StrategyDecisionHistoryPort {
    fun latest(limit: Int): List<StrategyDecisionRecord>
}
