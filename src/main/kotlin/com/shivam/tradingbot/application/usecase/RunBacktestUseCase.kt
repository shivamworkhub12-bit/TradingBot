package com.shivam.tradingbot.application.usecase

import com.shivam.tradingbot.application.port.out.MarketDataPort
import com.shivam.tradingbot.domain.model.OrderIntent
import com.shivam.tradingbot.domain.model.OrderSide
import com.shivam.tradingbot.domain.model.PaperFill
import com.shivam.tradingbot.domain.model.PaperPortfolio
import com.shivam.tradingbot.domain.model.Signal
import com.shivam.tradingbot.domain.model.SignalAction
import com.shivam.tradingbot.domain.cost.NoTransactionCosts
import com.shivam.tradingbot.domain.cost.TransactionCostCalculator
import com.shivam.tradingbot.domain.cost.NoSlippage
import com.shivam.tradingbot.domain.cost.SlippageModel
import com.shivam.tradingbot.domain.risk.RiskDecision
import com.shivam.tradingbot.domain.risk.RiskManager
import com.shivam.tradingbot.domain.strategy.TradingStrategy
import java.math.BigDecimal
import java.util.UUID

/**
 * Replays completed candles one at a time. It uses exactly the same strategy
 * and risk policy as paper trading, but never writes orders or portfolio state.
 */
class RunBacktestUseCase(
    private val marketData: MarketDataPort,
    private val strategy: TradingStrategy,
    private val riskManager: RiskManager,
    private val transactionCostCalculator: TransactionCostCalculator = NoTransactionCosts,
    private val slippageModel: SlippageModel = NoSlippage,
) {
    fun execute(command: RunBacktestCommand): BacktestResult {
        val candles = marketData.loadClosedCandles(command.symbol, command.candleCount)
        return execute(command, candles)
    }

    /** Runs a backtest against supplied completed candles, for example from Kite. */
    fun execute(
        command: RunBacktestCommand,
        candles: List<com.shivam.tradingbot.domain.model.Candle>,
        strategyToRun: TradingStrategy = strategy,
    ): BacktestResult {
        require(command.symbol.isNotBlank()) { "symbol must not be blank" }
        require(command.candleCount > 0) { "candleCount must be positive" }
        require(command.buyQuantity > 0) { "buyQuantity must be positive" }
        require(command.startingCash > BigDecimal.ZERO) { "startingCash must be positive" }
        require(candles.isNotEmpty()) { "at least one completed candle is required" }
        require(candles.all { it.symbol == command.symbol }) { "all candles must belong to ${command.symbol}" }

        var portfolio = PaperPortfolio(command.startingCash, BigDecimal.ZERO)
        val signals = mutableListOf<Signal>()
        val fills = mutableListOf<PaperFill>()
        var pendingOrder: PendingOrder? = null
        var highestEquity = command.startingCash
        var maximumDrawdown = BigDecimal.ZERO
        var winningTrades = 0
        var closedTrades = 0

        candles.indices.forEach { index ->
            val candle = candles[index]
            pendingOrder?.let { pending ->
                val marketOrder = OrderIntent(command.symbol, pending.side, pending.quantity, candle.open)
                val executionPrice = slippageModel.apply(marketOrder, candle.open)
                val order = marketOrder.copy(expectedPrice = executionPrice)
                if (riskManager.assess(order, portfolio) is RiskDecision.Approved) {
                    val previousAveragePrice = portfolio.positions[command.symbol]?.averagePrice
                    val fill = PaperFill(
                        UUID.randomUUID(),
                        order,
                        executionPrice,
                        candle.closedAt,
                        transactionCostCalculator.estimate(order, executionPrice),
                    )
                    portfolio = portfolio.apply(fill)
                    fills += fill
                    if (pending.side == OrderSide.SELL && previousAveragePrice != null) {
                        closedTrades++
                        if (fill.fillPrice > previousAveragePrice) winningTrades++
                    }
                }
            }
            pendingOrder = null

            val equity = portfolio.availableCash + portfolio.positions.values.sumOf { position ->
                candle.close.multiply(BigDecimal(position.quantity))
            }
            highestEquity = maxOf(highestEquity, equity)
            maximumDrawdown = maxOf(maximumDrawdown, highestEquity.subtract(equity))

            val signal = strategyToRun.evaluate(candles.subList(0, index + 1))
            signals += signal
            pendingOrder = signal.toPendingOrder(portfolio, command.symbol, command.buyQuantity)
        }

        if (command.closeOpenPositionAtEnd) {
            portfolio.positions[command.symbol]?.let { position ->
                val marketOrder = OrderIntent(command.symbol, OrderSide.SELL, position.quantity, candles.last().close)
                val executionPrice = slippageModel.apply(marketOrder, candles.last().close)
                val order = marketOrder.copy(expectedPrice = executionPrice)
                if (riskManager.assess(order, portfolio) is RiskDecision.Approved) {
                    val fill = PaperFill(
                        UUID.randomUUID(),
                        order,
                        executionPrice,
                        candles.last().closedAt,
                        transactionCostCalculator.estimate(order, executionPrice),
                    )
                    if (fill.fillPrice > position.averagePrice) winningTrades++
                    closedTrades++
                    portfolio = portfolio.apply(fill)
                    fills += fill
                    maximumDrawdown = maxOf(maximumDrawdown, highestEquity.subtract(portfolio.availableCash))
                }
            }
        }

        val endingEquity = portfolio.availableCash + portfolio.positions.values.sumOf { position ->
            candles.last().close.multiply(BigDecimal(position.quantity))
        }
        val realizedProfitLoss = portfolio.dailyRealizedProfitLoss

        return BacktestResult(
            symbol = command.symbol,
            startingCash = command.startingCash,
            finalPortfolio = portfolio,
            endingEquity = endingEquity,
            totalProfitLoss = endingEquity.subtract(command.startingCash),
            unrealizedProfitLoss = endingEquity.subtract(command.startingCash).subtract(realizedProfitLoss),
            signals = signals,
            fills = fills,
            closedTrades = closedTrades,
            winningTrades = winningTrades,
            maximumDrawdown = maximumDrawdown,
            transactionCosts = fills.fold(BigDecimal.ZERO) { total, fill -> total.add(fill.transactionCost) },
            slippageBasisPoints = slippageModel.basisPoints,
            closeOpenPositionAtEnd = command.closeOpenPositionAtEnd,
        )
    }

    private fun Signal.toPendingOrder(
        portfolio: PaperPortfolio,
        symbol: String,
        buyQuantity: Int,
    ): PendingOrder? = when (action) {
        SignalAction.BUY -> if (portfolio.positions.containsKey(symbol)) null else PendingOrder(OrderSide.BUY, buyQuantity)
        SignalAction.SELL -> portfolio.positions[symbol]?.let { PendingOrder(OrderSide.SELL, it.quantity) }
        SignalAction.HOLD -> null
    }

    private data class PendingOrder(val side: OrderSide, val quantity: Int)
}

data class RunBacktestCommand(
    val symbol: String,
    val candleCount: Int = 250,
    val buyQuantity: Int = 1,
    val startingCash: BigDecimal = BigDecimal("100000"),
    val closeOpenPositionAtEnd: Boolean = false,
)

data class BacktestResult(
    val symbol: String,
    val startingCash: BigDecimal,
    val finalPortfolio: PaperPortfolio,
    val endingEquity: BigDecimal,
    val totalProfitLoss: BigDecimal,
    val unrealizedProfitLoss: BigDecimal,
    val signals: List<Signal>,
    val fills: List<PaperFill>,
    val closedTrades: Int,
    val winningTrades: Int,
    val maximumDrawdown: BigDecimal,
    val transactionCosts: BigDecimal,
    val slippageBasisPoints: Int,
    val closeOpenPositionAtEnd: Boolean,
)
