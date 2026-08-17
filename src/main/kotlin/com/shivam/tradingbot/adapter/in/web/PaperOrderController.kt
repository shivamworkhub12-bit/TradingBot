package com.shivam.tradingbot.adapter.`in`.web

import com.shivam.tradingbot.application.usecase.PaperOrderResult
import com.shivam.tradingbot.application.usecase.PlacePaperOrderUseCase
import com.shivam.tradingbot.domain.model.OrderIntent
import com.shivam.tradingbot.domain.model.OrderSide
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/paper-orders")
class PaperOrderController(
    private val placePaperOrder: PlacePaperOrderUseCase,
) {
    @PostMapping
    fun place(
        @RequestParam symbol: String,
        @RequestParam side: OrderSide,
        @RequestParam quantity: Int,
        @RequestParam expectedPrice: BigDecimal,
    ): ResponseEntity<PaperOrderResponse> {
        val result = placePaperOrder.execute(OrderIntent(symbol, side, quantity, expectedPrice))
        return when (result) {
            is PaperOrderResult.Filled -> ResponseEntity.status(HttpStatus.CREATED).body(
                PaperOrderResponse(
                    status = "FILLED",
                    reason = "Paper order filled immediately at expected price",
                    fillId = result.fill.id,
                    fillPrice = result.fill.fillPrice,
                    filledAt = result.fill.filledAt,
                ),
            )
            is PaperOrderResult.Rejected -> ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(
                PaperOrderResponse(status = "REJECTED", reason = result.reason),
            )
        }
    }
}

data class PaperOrderResponse(
    val status: String,
    val reason: String,
    val fillId: UUID? = null,
    val fillPrice: BigDecimal? = null,
    val filledAt: Instant? = null,
)
