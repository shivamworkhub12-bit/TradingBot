package com.shivam.tradingbot.domain.risk

sealed interface RiskDecision {
    data class Approved(val reason: String) : RiskDecision
    data class Rejected(val reason: String) : RiskDecision
}
