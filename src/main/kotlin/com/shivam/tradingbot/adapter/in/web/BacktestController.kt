package com.shivam.tradingbot.adapter.`in`.web

import com.shivam.tradingbot.application.usecase.BacktestResult
import com.shivam.tradingbot.application.usecase.RunBacktestCommand
import com.shivam.tradingbot.application.usecase.RunBacktestUseCase
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

@RestController
@RequestMapping("/backtests")
class BacktestController(
    private val runBacktest: RunBacktestUseCase,
) {
    @PostMapping("/run")
    fun run(
        @RequestParam symbol: String,
        @RequestParam(defaultValue = "250") candleCount: Int,
        @RequestParam(defaultValue = "1") buyQuantity: Int,
        @RequestParam(defaultValue = "100000") startingCash: BigDecimal,
    ): BacktestResponse = BacktestResponse.from(
        runBacktest.execute(RunBacktestCommand(symbol, candleCount, buyQuantity, startingCash)),
    )
}

data class BacktestResponse(
    val symbol: String,
    val startingCash: BigDecimal,
    val endingCash: BigDecimal,
    val endingEquity: BigDecimal,
    val realizedProfitLoss: BigDecimal,
    val unrealizedProfitLoss: BigDecimal,
    val totalProfitLoss: BigDecimal,
    val openPositions: List<PaperPositionResponse>,
    val signalsEvaluated: Int,
    val closedTrades: Int,
    val winningTrades: Int,
    val winRatePercent: BigDecimal,
    val maximumDrawdown: BigDecimal,
    val transactionCosts: BigDecimal,
    val slippageBasisPoints: Int,
    val shortWindow: Int? = null,
    val longWindow: Int? = null,
    val closeOpenPositionAtEnd: Boolean? = null,
    val fills: List<PaperFillResponse>,
) {
    companion object {
        fun from(result: BacktestResult) = BacktestResponse(
            symbol = result.symbol,
            startingCash = result.startingCash,
            endingCash = result.finalPortfolio.availableCash,
            endingEquity = result.endingEquity,
            realizedProfitLoss = result.finalPortfolio.dailyRealizedProfitLoss,
            unrealizedProfitLoss = result.unrealizedProfitLoss,
            totalProfitLoss = result.totalProfitLoss,
            openPositions = result.finalPortfolio.positions.values.sortedBy { it.symbol }.map {
                PaperPositionResponse(it.symbol, it.quantity, it.averagePrice)
            },
            signalsEvaluated = result.signals.size,
            closedTrades = result.closedTrades,
            winningTrades = result.winningTrades,
            winRatePercent = if (result.closedTrades == 0) BigDecimal.ZERO else BigDecimal(result.winningTrades)
                .multiply(BigDecimal("100"))
                .divide(BigDecimal(result.closedTrades), 2, java.math.RoundingMode.HALF_UP),
            maximumDrawdown = result.maximumDrawdown,
            transactionCosts = result.transactionCosts,
            slippageBasisPoints = result.slippageBasisPoints,
            closeOpenPositionAtEnd = result.closeOpenPositionAtEnd,
            fills = result.fills.map(PaperFillResponse::from),
        )
    }
}
