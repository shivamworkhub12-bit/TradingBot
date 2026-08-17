package com.shivam.tradingbot.application.port.out

import com.shivam.tradingbot.domain.model.Candle

/**
 * The application's requirement for price history.
 *
 * An adapter will later implement this with generated data, a database, or a
 * market-data provider. The use case does not need to know which one.
 */
fun interface MarketDataPort {
    fun loadClosedCandles(symbol: String, limit: Int): List<Candle>
}
