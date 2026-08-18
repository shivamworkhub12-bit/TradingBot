package com.shivam.tradingbot.adapter.out.persistence

import com.shivam.tradingbot.application.port.out.StrategyDecisionHistoryPort
import com.shivam.tradingbot.application.port.out.StrategyDecisionStorePort
import com.shivam.tradingbot.domain.fno.OptionType
import com.shivam.tradingbot.domain.strategy.IntradayDirection
import com.shivam.tradingbot.domain.strategy.StrategyDecisionRecord
import com.shivam.tradingbot.domain.strategy.StrategyExecutionStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import java.sql.Date
import java.sql.Timestamp

class JdbcStrategyDecisionJournal(private val jdbc: JdbcTemplate) :
    StrategyDecisionStorePort,
    StrategyDecisionHistoryPort {

    override fun save(decision: StrategyDecisionRecord) {
        jdbc.update(
            """
            INSERT INTO fno_strategy_decisions (
                strategy_name, symbol, candle_closed_at, evaluated_at, direction,
                fast_ema, slow_ema, rsi, underlying_price, option_type, expiry,
                strike, trading_symbol, execution_status, reason
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (strategy_name, symbol, candle_closed_at) DO UPDATE SET
                evaluated_at = EXCLUDED.evaluated_at,
                direction = EXCLUDED.direction,
                fast_ema = EXCLUDED.fast_ema,
                slow_ema = EXCLUDED.slow_ema,
                rsi = EXCLUDED.rsi,
                underlying_price = EXCLUDED.underlying_price,
                option_type = EXCLUDED.option_type,
                expiry = EXCLUDED.expiry,
                strike = EXCLUDED.strike,
                trading_symbol = EXCLUDED.trading_symbol,
                execution_status = EXCLUDED.execution_status,
                reason = EXCLUDED.reason
            """.trimIndent(),
            decision.strategyName,
            decision.symbol,
            Timestamp.from(decision.candleClosedAt),
            Timestamp.from(decision.evaluatedAt),
            decision.direction.name,
            decision.fastEma,
            decision.slowEma,
            decision.rsi,
            decision.underlyingPrice,
            decision.optionType?.name,
            decision.expiry?.let(Date::valueOf),
            decision.strike,
            decision.tradingSymbol,
            decision.executionStatus.name,
            decision.reason,
        )
    }

    override fun latest(limit: Int, symbol: String?): List<StrategyDecisionRecord> = if (symbol == null) {
        jdbc.query(
            """
            SELECT strategy_name, symbol, candle_closed_at, evaluated_at, direction,
                   fast_ema, slow_ema, rsi, underlying_price, option_type, expiry,
                   strike, trading_symbol, execution_status, reason
            FROM fno_strategy_decisions
            ORDER BY candle_closed_at DESC
            LIMIT ?
            """.trimIndent(),
            rowMapper,
            limit,
        )
    } else {
        jdbc.query(
            """
            SELECT strategy_name, symbol, candle_closed_at, evaluated_at, direction,
                   fast_ema, slow_ema, rsi, underlying_price, option_type, expiry,
                   strike, trading_symbol, execution_status, reason
            FROM fno_strategy_decisions
            WHERE symbol = ?
            ORDER BY candle_closed_at DESC
            LIMIT ?
            """.trimIndent(),
            rowMapper,
            symbol,
            limit,
        )
    }

    private companion object {
        val rowMapper = RowMapper<StrategyDecisionRecord> { rs, _ ->
            StrategyDecisionRecord(
                strategyName = rs.getString("strategy_name"),
                symbol = rs.getString("symbol"),
                candleClosedAt = rs.getTimestamp("candle_closed_at").toInstant(),
                evaluatedAt = rs.getTimestamp("evaluated_at").toInstant(),
                direction = IntradayDirection.valueOf(rs.getString("direction")),
                fastEma = rs.getBigDecimal("fast_ema"),
                slowEma = rs.getBigDecimal("slow_ema"),
                rsi = rs.getBigDecimal("rsi"),
                underlyingPrice = rs.getBigDecimal("underlying_price"),
                optionType = rs.getString("option_type")?.let(OptionType::valueOf),
                expiry = rs.getDate("expiry")?.toLocalDate(),
                strike = rs.getBigDecimal("strike"),
                tradingSymbol = rs.getString("trading_symbol"),
                executionStatus = StrategyExecutionStatus.valueOf(rs.getString("execution_status")),
                reason = rs.getString("reason"),
            )
        }
    }
}

class InMemoryStrategyDecisionJournal : StrategyDecisionStorePort, StrategyDecisionHistoryPort {
    private val decisions = linkedMapOf<Triple<String, String, java.time.Instant>, StrategyDecisionRecord>()

    override fun save(decision: StrategyDecisionRecord) {
        decisions[Triple(decision.strategyName, decision.symbol, decision.candleClosedAt)] = decision
    }

    override fun latest(limit: Int, symbol: String?): List<StrategyDecisionRecord> = decisions.values
        .filter { symbol == null || it.symbol == symbol }
        .sortedByDescending { it.candleClosedAt }
        .take(limit)
}
