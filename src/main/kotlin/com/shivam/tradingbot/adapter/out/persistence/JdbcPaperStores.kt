package com.shivam.tradingbot.adapter.out.persistence

import com.shivam.tradingbot.application.port.out.PaperFillStorePort
import com.shivam.tradingbot.application.port.out.PaperFillHistoryPort
import com.shivam.tradingbot.application.port.out.PaperPortfolioStorePort
import com.shivam.tradingbot.domain.model.OrderIntent
import com.shivam.tradingbot.domain.model.OrderSide
import com.shivam.tradingbot.domain.model.PaperFill
import com.shivam.tradingbot.domain.model.PaperPortfolio
import com.shivam.tradingbot.domain.model.PaperPosition
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.UUID

class JdbcPaperPortfolioStoreAdapter(
    private val jdbcTemplate: JdbcTemplate,
) : PaperPortfolioStorePort {
    override fun load(): PaperPortfolio {
        val portfolio = jdbcTemplate.query(
            "SELECT available_cash, daily_realized_profit_loss FROM paper_portfolio WHERE id = TRUE",
            portfolioRowMapper,
        ).singleOrNull() ?: error("Paper portfolio has not been initialized")

        val positions = jdbcTemplate.query(
            "SELECT symbol, quantity, average_price FROM paper_positions ORDER BY symbol",
            positionRowMapper,
        ).associateBy { it.symbol }

        return portfolio.copy(positions = positions)
    }

    override fun save(portfolio: PaperPortfolio) {
        jdbcTemplate.update(
            """
            UPDATE paper_portfolio
            SET available_cash = ?, daily_realized_profit_loss = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = TRUE
            """.trimIndent(),
            portfolio.availableCash,
            portfolio.dailyRealizedProfitLoss,
        )
        jdbcTemplate.update("DELETE FROM paper_positions")
        portfolio.positions.values.forEach { position ->
            jdbcTemplate.update(
                """
                INSERT INTO paper_positions (symbol, quantity, average_price)
                VALUES (?, ?, ?)
                """.trimIndent(),
                position.symbol,
                position.quantity,
                position.averagePrice,
            )
        }
    }

    private companion object {
        val portfolioRowMapper = RowMapper { resultSet: ResultSet, _: Int ->
            PaperPortfolio(
                availableCash = resultSet.getBigDecimal("available_cash"),
                dailyRealizedProfitLoss = resultSet.getBigDecimal("daily_realized_profit_loss"),
            )
        }
        val positionRowMapper = RowMapper { resultSet: ResultSet, _: Int ->
            PaperPosition(
                symbol = resultSet.getString("symbol"),
                quantity = resultSet.getInt("quantity"),
                averagePrice = resultSet.getBigDecimal("average_price"),
            )
        }
    }
}

class JdbcPaperFillStoreAdapter(
    private val jdbcTemplate: JdbcTemplate,
) : PaperFillStorePort, PaperFillHistoryPort {
    override fun save(fill: PaperFill) {
        jdbcTemplate.update(
            """
            INSERT INTO paper_fills (id, symbol, side, quantity, fill_price, transaction_cost, filled_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            fill.id,
            fill.order.symbol,
            fill.order.side.name,
            fill.order.quantity,
            fill.fillPrice,
            fill.transactionCost,
            Timestamp.from(fill.filledAt),
        )
    }

    override fun latest(limit: Int): List<PaperFill> = jdbcTemplate.query(
        """
        SELECT id, symbol, side, quantity, fill_price, transaction_cost, filled_at
        FROM paper_fills
        ORDER BY filled_at DESC
        LIMIT ?
        """.trimIndent(),
        RowMapper { resultSet, _ ->
            PaperFill(
                id = resultSet.getObject("id", UUID::class.java),
                order = OrderIntent(
                    symbol = resultSet.getString("symbol"),
                    side = OrderSide.valueOf(resultSet.getString("side")),
                    quantity = resultSet.getInt("quantity"),
                    expectedPrice = resultSet.getBigDecimal("fill_price"),
                ),
                fillPrice = resultSet.getBigDecimal("fill_price"),
                filledAt = resultSet.getTimestamp("filled_at").toInstant(),
                transactionCost = resultSet.getBigDecimal("transaction_cost"),
            )
        },
        limit,
    )
}
