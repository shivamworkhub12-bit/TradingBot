package com.shivam.tradingbot.adapter.out.marketdata

import kotlin.test.Test
import kotlin.test.assertEquals

class SampleNseMarketDataAdapterTest {
    @Test
    fun `returns the requested number of completed candles for the requested symbol`() {
        val candles = SampleNseMarketDataAdapter().loadClosedCandles("NSE:INFY", 4)

        assertEquals(4, candles.size)
        assertEquals(listOf("NSE:INFY"), candles.map { it.symbol }.distinct())
    }
}
