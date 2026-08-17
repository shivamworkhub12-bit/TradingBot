package com.shivam.tradingbot.adapter.`in`.web

import com.shivam.tradingbot.application.port.out.PaperFillHistoryPort
import com.shivam.tradingbot.domain.model.PaperFill
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/paper-fills")
class PaperFillHistoryController(
    private val paperFillHistory: PaperFillHistoryPort,
) {
    @GetMapping
    fun latest(@RequestParam(defaultValue = "20") limit: Int): List<PaperFillResponse> =
        paperFillHistory.latest(validatedLimit(limit)).map(PaperFillResponse::from)
}

data class PaperFillResponse(
    val id: UUID,
    val symbol: String,
    val side: String,
    val quantity: Int,
    val fillPrice: BigDecimal,
    val transactionCost: BigDecimal,
    val filledAt: Instant,
) {
    companion object {
        fun from(fill: PaperFill) = PaperFillResponse(
            id = fill.id,
            symbol = fill.order.symbol,
            side = fill.order.side.name,
            quantity = fill.order.quantity,
            fillPrice = fill.fillPrice,
            transactionCost = fill.transactionCost,
            filledAt = fill.filledAt,
        )
    }
}
