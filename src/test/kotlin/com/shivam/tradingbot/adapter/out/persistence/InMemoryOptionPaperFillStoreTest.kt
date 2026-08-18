package com.shivam.tradingbot.adapter.out.persistence

import com.shivam.tradingbot.domain.fno.IndexUnderlying
import com.shivam.tradingbot.domain.fno.OptionContract
import com.shivam.tradingbot.domain.fno.OptionOrderIntent
import com.shivam.tradingbot.domain.fno.OptionPaperFill
import com.shivam.tradingbot.domain.fno.OptionType
import com.shivam.tradingbot.domain.model.OrderSide
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InMemoryOptionPaperFillStoreTest {
    @Test
    fun `daily fill limit is tracked independently by underlying`() {
        val date = LocalDate.of(2026, 8, 18)
        val store = InMemoryOptionPaperFillStore()
        val contract = OptionContract(
            underlying = IndexUnderlying.NIFTY,
            expiry = LocalDate.of(2026, 8, 27),
            strike = BigDecimal("24200"),
            optionType = OptionType.CE,
            lotSize = 65,
            tradingSymbol = "NIFTY26AUG24200CE",
        )
        store.save(
            OptionPaperFill(
                id = UUID.randomUUID(),
                order = OptionOrderIntent(contract, OrderSide.BUY, 1, BigDecimal("100")),
                filledAt = Instant.parse("2026-08-18T09:00:00Z"),
            ),
        )

        assertTrue(store.hasFillOn(date, IndexUnderlying.NIFTY))
        assertFalse(store.hasFillOn(date, IndexUnderlying.BANKNIFTY))
    }
}
