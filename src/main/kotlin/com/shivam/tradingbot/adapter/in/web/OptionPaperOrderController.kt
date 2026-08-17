package com.shivam.tradingbot.adapter.`in`.web

import com.shivam.tradingbot.application.usecase.OptionPaperOrderResult
import com.shivam.tradingbot.application.usecase.PlaceOptionPaperOrderUseCase
import com.shivam.tradingbot.application.usecase.CloseOptionPaperPositionUseCase
import com.shivam.tradingbot.domain.fno.IndexUnderlying
import com.shivam.tradingbot.domain.fno.OptionContract
import com.shivam.tradingbot.domain.fno.OptionOrderIntent
import com.shivam.tradingbot.domain.fno.OptionType
import com.shivam.tradingbot.domain.model.OrderSide
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.LocalDate

/** Creates a local long-option paper position. This does not call Kite orders. */
@RestController
@RequestMapping("/fno/paper-orders")
class OptionPaperOrderController(
    private val placeOptionPaperOrder: PlaceOptionPaperOrderUseCase,
    private val closeOptionPaperPosition: CloseOptionPaperPositionUseCase,
) {
    @PostMapping
    fun place(
        @RequestParam underlying: IndexUnderlying, @RequestParam expiry: LocalDate, @RequestParam strike: BigDecimal,
        @RequestParam optionType: OptionType, @RequestParam lotSize: Int, @RequestParam tradingSymbol: String,
        @RequestParam side: OrderSide, @RequestParam lots: Int, @RequestParam expectedPremium: BigDecimal,
        @RequestParam asOf: LocalDate,
    ): ResponseEntity<OptionPaperOrderResponse> {
        val result = placeOptionPaperOrder.execute(
            OptionOrderIntent(OptionContract(underlying, expiry, strike, optionType, lotSize, tradingSymbol), side, lots, expectedPremium), asOf)
        return when (result) {
            is OptionPaperOrderResult.Filled -> ResponseEntity.status(HttpStatus.CREATED).body(
                OptionPaperOrderResponse("FILLED", "Long option paper position created", result.fill.id.toString(), result.fill.order.quantity, result.fill.order.premiumExposure))
            is OptionPaperOrderResult.Rejected -> ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(OptionPaperOrderResponse("REJECTED", result.reason))
        }
    }

    @PostMapping("/close")
    fun close(@RequestParam tradingSymbol: String, @RequestParam exitPremium: BigDecimal): ResponseEntity<OptionPaperOrderResponse> =
        when (val result = closeOptionPaperPosition.execute(tradingSymbol, exitPremium)) {
            is OptionPaperOrderResult.Filled -> ResponseEntity.status(HttpStatus.CREATED).body(
                OptionPaperOrderResponse("FILLED", "Long option paper position closed", result.fill.id.toString(), result.fill.order.quantity, result.fill.order.premiumExposure),
            )
            is OptionPaperOrderResult.Rejected -> ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(OptionPaperOrderResponse("REJECTED", result.reason))
        }
}

data class OptionPaperOrderResponse(val status: String, val reason: String, val fillId: String? = null, val quantity: Int? = null, val premiumPaid: BigDecimal? = null)
