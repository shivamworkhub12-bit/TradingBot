package com.shivam.tradingbot.application.usecase

import com.shivam.tradingbot.application.port.out.MarketDataPort
import com.shivam.tradingbot.domain.model.Candle
import com.shivam.tradingbot.domain.model.Signal
import com.shivam.tradingbot.domain.model.SignalAction
import com.shivam.tradingbot.domain.risk.FixedLimitRiskManager
import com.shivam.tradingbot.domain.strategy.TradingStrategy
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class RunBacktestUseCaseTest {
    @Test
    fun `replays a buy signal and returns the resulting portfolio`() {
        val useCase = RunBacktestUseCase(
            marketData = MarketDataPort { _, _ -> candles() },
            strategy = TradingStrategy { history ->
                Signal(
                    symbol = "NSE:INFY",
                    action = if (history.size == 2) SignalAction.BUY else SignalAction.HOLD,
                    generatedAt = history.last().closedAt,
                    reason = "test strategy",
                )
            },
            riskManager = FixedLimitRiskManager(BigDecimal("10000"), BigDecimal("500")),
        )

        val result = useCase.execute(RunBacktestCommand("NSE:INFY", candleCount = 3, buyQuantity = 2))

        assertEquals(1, result.fills.size)
        assertEquals(BigDecimal("99798"), result.finalPortfolio.availableCash)
        assertEquals(2, result.finalPortfolio.positions.getValue("NSE:INFY").quantity)
    }

    private fun candles() = listOf(100, 100, 101).mapIndexed { index, close ->
        Candle(
            symbol = "NSE:INFY",
            closedAt = Instant.EPOCH.plusSeconds(index * 60L),
            open = BigDecimal(close),
            high = BigDecimal(close),
            low = BigDecimal(close),
            close = BigDecimal(close),
            volume = 1,
        )
    }
}
