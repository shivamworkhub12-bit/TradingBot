package com.shivam.tradingbot.domain.strategy

import com.shivam.tradingbot.domain.fno.OptionType
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

enum class StrategyExecutionStatus {
    NO_SIGNAL,
    SIGNAL_DETECTED,
    PREMIUM_REJECTED,
    MARKET_DATA_ERROR,
    PAPER_ORDER_FILLED,
    PAPER_ORDER_REJECTED,
}

/** A durable explanation of what the strategy observed and what the application did. */
data class StrategyDecisionRecord(
    val strategyName: String,
    val symbol: String,
    val candleClosedAt: Instant,
    val evaluatedAt: Instant,
    val direction: IntradayDirection,
    val fastEma: BigDecimal?,
    val slowEma: BigDecimal?,
    val rsi: BigDecimal?,
    val underlyingPrice: BigDecimal,
    val optionType: OptionType? = null,
    val expiry: LocalDate? = null,
    val strike: BigDecimal? = null,
    val tradingSymbol: String? = null,
    val executionStatus: StrategyExecutionStatus,
    val reason: String,
)
