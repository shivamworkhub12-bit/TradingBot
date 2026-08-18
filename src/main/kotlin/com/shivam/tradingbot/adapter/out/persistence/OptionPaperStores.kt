package com.shivam.tradingbot.adapter.out.persistence

import com.shivam.tradingbot.application.port.out.OptionPaperFillStorePort
import com.shivam.tradingbot.application.port.out.OptionPaperPortfolioStorePort
import com.shivam.tradingbot.domain.fno.IndexUnderlying
import com.shivam.tradingbot.domain.fno.OptionContract
import com.shivam.tradingbot.domain.fno.OptionPaperFill
import com.shivam.tradingbot.domain.fno.OptionPaperPortfolio
import com.shivam.tradingbot.domain.fno.OptionPaperPosition
import com.shivam.tradingbot.domain.fno.OptionType
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.Date
import java.sql.Timestamp
import java.time.LocalDate
import java.time.ZoneId

class JdbcOptionPaperPortfolioStore(private val jdbc: JdbcTemplate) : OptionPaperPortfolioStorePort {
    override fun load(): OptionPaperPortfolio {
        val summary = jdbc.queryForMap("SELECT available_cash, realized_profit_loss FROM fno_paper_portfolio WHERE id = TRUE")
        val cash = summary.getValue("available_cash") as java.math.BigDecimal
        val realized = summary.getValue("realized_profit_loss") as java.math.BigDecimal
        val positions = jdbc.query("SELECT * FROM fno_paper_positions") { rs, _ ->
            val contract = OptionContract(
                IndexUnderlying.valueOf(rs.getString("underlying")),
                rs.getDate("expiry").toLocalDate(),
                rs.getBigDecimal("strike"),
                OptionType.valueOf(rs.getString("option_type")),
                rs.getInt("lot_size"),
                rs.getString("trading_symbol"),
            )
            OptionPaperPosition(contract, rs.getInt("lots"), rs.getBigDecimal("average_premium"))
        }.associateBy { it.contract.tradingSymbol }
        return OptionPaperPortfolio(cash, realized, positions)
    }

    override fun save(portfolio: OptionPaperPortfolio) {
        jdbc.update("UPDATE fno_paper_portfolio SET available_cash = ?, realized_profit_loss = ?, updated_at = CURRENT_TIMESTAMP WHERE id = TRUE", portfolio.availableCash, portfolio.realizedProfitLoss)
        jdbc.update("DELETE FROM fno_paper_positions")
        portfolio.positions.values.forEach { p ->
            jdbc.update("""INSERT INTO fno_paper_positions
                (trading_symbol, underlying, expiry, strike, option_type, lot_size, lots, average_premium)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)""".trimIndent(),
                p.contract.tradingSymbol, p.contract.underlying.name, Date.valueOf(p.contract.expiry), p.contract.strike,
                p.contract.optionType.name, p.contract.lotSize, p.lots, p.averagePremium)
        }
    }
}

class JdbcOptionPaperFillStore(private val jdbc: JdbcTemplate) : OptionPaperFillStorePort {
    override fun save(fill: OptionPaperFill) {
        val c = fill.order.contract
        jdbc.update("""INSERT INTO fno_paper_fills
            (id, trading_symbol, underlying, expiry, strike, option_type, lot_size, side, lots, premium, filled_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""".trimIndent(),
            fill.id, c.tradingSymbol, c.underlying.name, Date.valueOf(c.expiry), c.strike, c.optionType.name,
            c.lotSize, fill.order.side.name, fill.order.lots, fill.order.expectedPremium, Timestamp.from(fill.filledAt))
    }

    override fun hasFillOn(date: LocalDate, underlying: IndexUnderlying): Boolean {
        val zone = ZoneId.of("Asia/Kolkata")
        val from = Timestamp.from(date.atStartOfDay(zone).toInstant())
        val until = Timestamp.from(date.plusDays(1).atStartOfDay(zone).toInstant())
        return jdbc.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM fno_paper_fills WHERE filled_at >= ? AND filled_at < ? AND underlying = ?)",
            Boolean::class.java,
            from,
            until,
            underlying.name,
        ) == true
    }
}

class InMemoryOptionPaperPortfolioStore(initial: OptionPaperPortfolio) : OptionPaperPortfolioStorePort {
    private var portfolio = initial
    override fun load() = portfolio
    override fun save(portfolio: OptionPaperPortfolio) { this.portfolio = portfolio }
}

class InMemoryOptionPaperFillStore : OptionPaperFillStorePort {
    val fills = mutableListOf<OptionPaperFill>()
    override fun save(fill: OptionPaperFill) { fills += fill }
    override fun hasFillOn(date: LocalDate, underlying: IndexUnderlying): Boolean = fills.any {
        it.order.contract.underlying == underlying &&
            it.filledAt.atZone(ZoneId.of("Asia/Kolkata")).toLocalDate() == date
    }
}
