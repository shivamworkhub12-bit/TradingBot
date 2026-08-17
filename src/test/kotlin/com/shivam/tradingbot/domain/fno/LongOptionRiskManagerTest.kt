package com.shivam.tradingbot.domain.fno

import com.shivam.tradingbot.domain.model.OrderSide
import com.shivam.tradingbot.domain.risk.RiskDecision
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertIs

class LongOptionRiskManagerTest {
    private val manager = LongOptionRiskManager()
    private val contract = OptionContract(
        underlying = IndexUnderlying.NIFTY,
        expiry = LocalDate.parse("2026-09-24"),
        strike = BigDecimal("25000"),
        optionType = OptionType.PE,
        lotSize = 65,
        tradingSymbol = "NIFTY26SEP25000PE",
    )

    @Test
    fun `approves an affordable long option`() {
        val decision = manager.assess(
            OptionOrderIntent(contract, OrderSide.BUY, lots = 1, expectedPremium = BigDecimal("100")),
            availableCash = BigDecimal("100000"),
            dailyRealizedProfitLoss = BigDecimal.ZERO,
            asOf = LocalDate.parse("2026-08-17"),
        )

        assertIs<RiskDecision.Approved>(decision)
    }

    @Test
    fun `rejects naked option selling`() {
        val decision = manager.assess(
            OptionOrderIntent(contract, OrderSide.SELL, lots = 1, expectedPremium = BigDecimal("100")),
            availableCash = BigDecimal("100000"),
            dailyRealizedProfitLoss = BigDecimal.ZERO,
            asOf = LocalDate.parse("2026-08-17"),
        )

        assertIs<RiskDecision.Rejected>(decision)
    }
}
