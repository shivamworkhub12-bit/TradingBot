package com.shivam.tradingbot.adapter.out.persistence

import com.shivam.tradingbot.domain.strategy.IntradayDirection
import com.shivam.tradingbot.domain.strategy.StrategyDecisionRecord
import com.shivam.tradingbot.domain.strategy.StrategyExecutionStatus
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class InMemoryStrategyDecisionJournalTest {
    @Test
    fun `updates the same strategy candle instead of creating duplicate polling rows`() {
        val journal = InMemoryStrategyDecisionJournal()
        val decision = decision(StrategyExecutionStatus.SIGNAL_DETECTED)

        journal.save(decision)
        journal.save(decision.copy(executionStatus = StrategyExecutionStatus.PAPER_ORDER_FILLED))

        val saved = journal.latest(20)
        assertEquals(1, saved.size)
        assertEquals(StrategyExecutionStatus.PAPER_ORDER_FILLED, saved.single().executionStatus)
    }

    @Test
    fun `returns newest candle first and respects limit`() {
        val journal = InMemoryStrategyDecisionJournal()
        val older = decision(StrategyExecutionStatus.NO_SIGNAL)
        val newer = older.copy(candleClosedAt = older.candleClosedAt.plusSeconds(300))

        journal.save(older)
        journal.save(newer)

        assertEquals(newer.candleClosedAt, journal.latest(1).single().candleClosedAt)
    }

    @Test
    fun `filters decisions by index symbol`() {
        val journal = InMemoryStrategyDecisionJournal()
        val nifty = decision(StrategyExecutionStatus.NO_SIGNAL)
        val bankNifty = nifty.copy(symbol = "NSE:NIFTY BANK")

        journal.save(nifty)
        journal.save(bankNifty)

        assertEquals(listOf(bankNifty), journal.latest(20, "NSE:NIFTY BANK"))
    }

    private fun decision(status: StrategyExecutionStatus) = StrategyDecisionRecord(
        strategyName = "EMA_9_21_RSI_14",
        symbol = "NSE:NIFTY 50",
        candleClosedAt = Instant.parse("2026-08-18T04:00:00Z"),
        evaluatedAt = Instant.parse("2026-08-18T04:05:00Z"),
        direction = IntradayDirection.BULLISH,
        fastEma = BigDecimal("24310"),
        slowEma = BigDecimal("24300"),
        rsi = BigDecimal("55"),
        underlyingPrice = BigDecimal("24320"),
        executionStatus = status,
        reason = "test decision",
    )
}
