package com.shivam.tradingbot.adapter.out.persistence

import com.shivam.tradingbot.application.port.out.SignalHistoryPort
import com.shivam.tradingbot.application.port.out.SignalStorePort
import com.shivam.tradingbot.domain.model.Signal
import com.shivam.tradingbot.domain.model.SignalAction
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import java.sql.Timestamp
import java.util.UUID

class JdbcSignalStoreAdapter(
    private val jdbcTemplate: JdbcTemplate,
) : SignalStorePort, SignalHistoryPort {
    override fun save(signal: Signal) {
        jdbcTemplate.update(
            """
            INSERT INTO strategy_signals (id, symbol, action, generated_at, reason)
            VALUES (?, ?, ?, ?, ?)
            """.trimIndent(),
            UUID.randomUUID(),
            signal.symbol,
            signal.action.name,
            Timestamp.from(signal.generatedAt),
            signal.reason,
        )
    }

    override fun latest(limit: Int): List<Signal> = jdbcTemplate.query(
        """
        SELECT symbol, action, generated_at, reason
        FROM strategy_signals
        ORDER BY generated_at DESC, created_at DESC
        LIMIT ?
        """.trimIndent(),
        RowMapper { resultSet, _ ->
            Signal(
                symbol = resultSet.getString("symbol"),
                action = SignalAction.valueOf(resultSet.getString("action")),
                generatedAt = resultSet.getTimestamp("generated_at").toInstant(),
                reason = resultSet.getString("reason"),
            )
        },
        limit,
    )
}
