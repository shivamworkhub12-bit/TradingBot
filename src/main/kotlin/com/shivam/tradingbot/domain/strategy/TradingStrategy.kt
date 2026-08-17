package com.shivam.tradingbot.domain.strategy

import com.shivam.tradingbot.domain.model.Candle
import com.shivam.tradingbot.domain.model.Signal

/** Pure business rule: price history in, recommendation out. */
fun interface TradingStrategy {
    fun evaluate(candles: List<Candle>): Signal
}
