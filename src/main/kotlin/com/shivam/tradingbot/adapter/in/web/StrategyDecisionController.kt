package com.shivam.tradingbot.adapter.`in`.web

import com.shivam.tradingbot.application.port.out.StrategyDecisionHistoryPort
import com.shivam.tradingbot.domain.fno.OptionType
import com.shivam.tradingbot.domain.strategy.IntradayDirection
import com.shivam.tradingbot.domain.strategy.StrategyDecisionRecord
import com.shivam.tradingbot.domain.strategy.StrategyExecutionStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@RestController
@RequestMapping("/fno/strategy-decisions")
class StrategyDecisionController(
    private val history: StrategyDecisionHistoryPort,
) {
    @GetMapping
    fun latest(
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(required = false) symbol: String?,
    ): List<StrategyDecisionResponse> = history
        .latest(validatedLimit(limit), symbol?.trim()?.takeIf(String::isNotEmpty))
        .map(StrategyDecisionResponse::from)
}

data class StrategyDecisionResponse(
    val strategyName: String,
    val symbol: String,
    val candleClosedAt: Instant,
    val evaluatedAt: Instant,
    val direction: IntradayDirection,
    val fastEma: BigDecimal?,
    val slowEma: BigDecimal?,
    val rsi: BigDecimal?,
    val underlyingPrice: BigDecimal,
    val optionType: OptionType?,
    val expiry: LocalDate?,
    val strike: BigDecimal?,
    val tradingSymbol: String?,
    val executionStatus: StrategyExecutionStatus,
    val reason: String,
) {
    companion object {
        fun from(decision: StrategyDecisionRecord) = StrategyDecisionResponse(
            strategyName = decision.strategyName,
            symbol = decision.symbol,
            candleClosedAt = decision.candleClosedAt,
            evaluatedAt = decision.evaluatedAt,
            direction = decision.direction,
            fastEma = decision.fastEma,
            slowEma = decision.slowEma,
            rsi = decision.rsi,
            underlyingPrice = decision.underlyingPrice,
            optionType = decision.optionType,
            expiry = decision.expiry,
            strike = decision.strike,
            tradingSymbol = decision.tradingSymbol,
            executionStatus = decision.executionStatus,
            reason = decision.reason,
        )
    }
}
