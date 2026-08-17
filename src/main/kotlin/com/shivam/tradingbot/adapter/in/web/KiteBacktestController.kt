package com.shivam.tradingbot.adapter.`in`.web

import com.shivam.tradingbot.application.port.out.KiteCandleInterval
import com.shivam.tradingbot.application.usecase.RunKiteBacktestCommand
import com.shivam.tradingbot.application.usecase.RunKiteBacktestUseCase
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.OffsetDateTime

/** Read-only endpoint: all trades it produces exist only in the returned backtest result. */
@RestController
@RequestMapping("/kite/backtests")
class KiteBacktestController(
    private val runKiteBacktest: RunKiteBacktestUseCase,
) {
    @PostMapping("/run")
    fun run(
        @RequestParam symbol: String,
        @RequestParam instrumentToken: Long,
        @RequestParam interval: KiteCandleInterval,
        @RequestParam from: String,
        @RequestParam to: String,
        @RequestParam(defaultValue = "1") buyQuantity: Int,
        @RequestParam(defaultValue = "100000") startingCash: BigDecimal,
        @RequestParam(defaultValue = "3") shortWindow: Int,
        @RequestParam(defaultValue = "10") longWindow: Int,
        @RequestParam(defaultValue = "false") closeOpenPositionAtEnd: Boolean,
    ): BacktestResponse {
        val result = runKiteBacktest.execute(
            RunKiteBacktestCommand(
                symbol = symbol,
                instrumentToken = instrumentToken,
                interval = interval,
                from = parseOffset(from).toInstant(),
                to = parseOffset(to).toInstant(),
                buyQuantity = buyQuantity,
                startingCash = startingCash,
                shortWindow = shortWindow,
                longWindow = longWindow,
                closeOpenPositionAtEnd = closeOpenPositionAtEnd,
            ),
        )
        return BacktestResponse.from(result).copy(
            shortWindow = shortWindow,
            longWindow = longWindow,
            closeOpenPositionAtEnd = closeOpenPositionAtEnd,
        )
    }

    private fun parseOffset(value: String): OffsetDateTime =
        OffsetDateTime.parse(value.replace(' ', '+'))
}
