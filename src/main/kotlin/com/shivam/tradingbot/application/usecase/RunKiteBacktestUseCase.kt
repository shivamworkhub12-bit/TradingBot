package com.shivam.tradingbot.application.usecase

import com.shivam.tradingbot.application.port.out.KiteCandleInterval
import com.shivam.tradingbot.application.port.out.KiteHistoricalDataPort
import com.shivam.tradingbot.application.port.out.KiteHistoricalDataRequest
import java.math.BigDecimal
import java.time.Instant
import com.shivam.tradingbot.domain.strategy.MovingAverageCrossoverStrategy

/** Fetches completed Kite candles, then performs an isolated simulated backtest. */
class RunKiteBacktestUseCase(
    private val kiteHistoricalData: KiteHistoricalDataPort,
    private val runBacktest: RunBacktestUseCase,
) {
    fun execute(command: RunKiteBacktestCommand): BacktestResult {
        require(command.instrumentToken > 0) { "instrumentToken must be positive" }
        require(command.from <= command.to) { "from must not be after to" }
        require(command.shortWindow > 0) { "shortWindow must be positive" }
        require(command.longWindow > command.shortWindow) { "longWindow must be greater than shortWindow" }

        val candles = kiteHistoricalData.load(
            KiteHistoricalDataRequest(
                symbol = command.symbol,
                instrumentToken = command.instrumentToken,
                interval = command.interval,
                from = command.from,
                to = command.to,
            ),
        )
        return runBacktest.execute(
            RunBacktestCommand(
                symbol = command.symbol,
                candleCount = candles.size,
                buyQuantity = command.buyQuantity,
                startingCash = command.startingCash,
                closeOpenPositionAtEnd = command.closeOpenPositionAtEnd,
            ),
            candles,
            MovingAverageCrossoverStrategy(command.shortWindow, command.longWindow),
        )
    }
}

data class RunKiteBacktestCommand(
    val symbol: String,
    val instrumentToken: Long,
    val interval: KiteCandleInterval,
    val from: Instant,
    val to: Instant,
    val buyQuantity: Int = 1,
    val startingCash: BigDecimal = BigDecimal("100000"),
    val shortWindow: Int = 3,
    val longWindow: Int = 10,
    val closeOpenPositionAtEnd: Boolean = false,
)
