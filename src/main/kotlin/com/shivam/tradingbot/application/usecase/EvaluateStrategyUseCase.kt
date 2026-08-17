package com.shivam.tradingbot.application.usecase

import com.shivam.tradingbot.application.port.out.MarketDataPort
import com.shivam.tradingbot.application.port.out.SignalStorePort
import com.shivam.tradingbot.domain.model.Signal
import com.shivam.tradingbot.domain.strategy.TradingStrategy

/**
 * Orchestrates one strategy evaluation.
 *
 * This is application logic: it coordinates dependencies, but it does not
 * contain the moving-average rule or know how data is fetched/stored.
 */
class EvaluateStrategyUseCase(
    private val marketData: MarketDataPort,
    private val signalStore: SignalStorePort,
    private val strategy: TradingStrategy,
    private val candleHistorySize: Int,
) {
    init {
        require(candleHistorySize > 0) { "candleHistorySize must be positive" }
    }

    fun execute(symbol: String): Signal {
        require(symbol.isNotBlank()) { "symbol must not be blank" }

        val candles = marketData.loadClosedCandles(symbol, candleHistorySize)
        val signal = strategy.evaluate(candles)
        signalStore.save(signal)
        return signal
    }
}
