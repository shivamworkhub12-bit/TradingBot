package com.shivam.tradingbot.domain.strategy

import com.shivam.tradingbot.domain.model.Candle
import com.shivam.tradingbot.domain.model.Signal
import com.shivam.tradingbot.domain.model.SignalAction
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Signals only when the short moving average crosses the long moving average.
 * Keeping this class framework-free makes it easy to test and reuse in a backtest
 * or later in a scheduled live-paper-trading job.
 */
class MovingAverageCrossoverStrategy(
    private val shortWindow: Int = 20,
    private val longWindow: Int = 50,
) : TradingStrategy {
    init {
        require(shortWindow > 0) { "shortWindow must be positive" }
        require(longWindow > shortWindow) { "longWindow must be greater than shortWindow" }
    }

    override fun evaluate(candles: List<Candle>): Signal {
        require(candles.isNotEmpty()) { "at least one candle is required" }
        require(candles.map { it.symbol }.distinct().size == 1) { "all candles must use one symbol" }

        val latest = candles.last()
        if (candles.size < longWindow + 1) {
            return hold(latest, "Not enough candles for $shortWindow/$longWindow moving averages")
        }

        val previous = candles.dropLast(1)
        val previousShort = averageClose(previous.takeLast(shortWindow))
        val previousLong = averageClose(previous.takeLast(longWindow))
        val currentShort = averageClose(candles.takeLast(shortWindow))
        val currentLong = averageClose(candles.takeLast(longWindow))

        val action = when {
            previousShort <= previousLong && currentShort > currentLong -> SignalAction.BUY
            previousShort >= previousLong && currentShort < currentLong -> SignalAction.SELL
            else -> SignalAction.HOLD
        }

        return Signal(
            symbol = latest.symbol,
            action = action,
            generatedAt = latest.closedAt,
            reason = "shortMA=$currentShort, longMA=$currentLong",
        )
    }

    private fun hold(latest: Candle, reason: String) = Signal(
        symbol = latest.symbol,
        action = SignalAction.HOLD,
        generatedAt = latest.closedAt,
        reason = reason,
    )

    private fun averageClose(candles: List<Candle>): BigDecimal =
        candles.map { it.close }
            .reduce(BigDecimal::add)
            .divide(BigDecimal(candles.size), 4, RoundingMode.HALF_UP)
}
