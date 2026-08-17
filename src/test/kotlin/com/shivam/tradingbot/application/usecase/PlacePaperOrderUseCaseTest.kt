package com.shivam.tradingbot.application.usecase

import com.shivam.tradingbot.application.port.out.PaperBrokerPort
import com.shivam.tradingbot.application.port.out.PaperFillStorePort
import com.shivam.tradingbot.application.port.out.PaperPortfolioStorePort
import com.shivam.tradingbot.domain.model.OrderIntent
import com.shivam.tradingbot.domain.model.OrderSide
import com.shivam.tradingbot.domain.model.PaperFill
import com.shivam.tradingbot.domain.model.PaperPortfolio
import com.shivam.tradingbot.domain.risk.FixedLimitRiskManager
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PlacePaperOrderUseCaseTest {
    @Test
    fun `fills an approved order and updates the paper portfolio`() {
        val portfolioStore = InMemoryPortfolioStore(PaperPortfolio(BigDecimal("10000"), BigDecimal.ZERO))
        val fillStore = RecordingFillStore()
        val useCase = PlacePaperOrderUseCase(
            portfolioStore = portfolioStore,
            paperBroker = FixedPaperBroker,
            fillStore = fillStore,
            riskManager = FixedLimitRiskManager(BigDecimal("5000"), BigDecimal("500")),
        )

        val result = useCase.execute(order(quantity = 2, price = "1000"))

        assertIs<PaperOrderResult.Filled>(result)
        assertEquals(BigDecimal("8000"), portfolioStore.portfolio.availableCash)
        assertEquals(2, portfolioStore.portfolio.positions.getValue("NSE:INFY").quantity)
        assertEquals(1, fillStore.fills.size)
    }

    @Test
    fun `does not fill an order rejected by risk management`() {
        val portfolioStore = InMemoryPortfolioStore(PaperPortfolio(BigDecimal("10000"), BigDecimal.ZERO))
        val fillStore = RecordingFillStore()
        val useCase = PlacePaperOrderUseCase(
            portfolioStore, FixedPaperBroker, fillStore, FixedLimitRiskManager(BigDecimal("5000"), BigDecimal("500")),
        )

        val result = useCase.execute(order(quantity = 6, price = "1000"))

        assertIs<PaperOrderResult.Rejected>(result)
        assertEquals(BigDecimal("10000"), portfolioStore.portfolio.availableCash)
        assertEquals(0, fillStore.fills.size)
    }

    private fun order(quantity: Int, price: String) = OrderIntent("NSE:INFY", OrderSide.BUY, quantity, BigDecimal(price))

    private object FixedPaperBroker : PaperBrokerPort {
        override fun fill(order: OrderIntent) = PaperFill(UUID.randomUUID(), order, order.expectedPrice, Instant.EPOCH)
    }

    private class InMemoryPortfolioStore(initial: PaperPortfolio) : PaperPortfolioStorePort {
        var portfolio = initial
        override fun load() = portfolio
        override fun save(portfolio: PaperPortfolio) { this.portfolio = portfolio }
    }

    private class RecordingFillStore : PaperFillStorePort {
        val fills = mutableListOf<PaperFill>()
        override fun save(fill: PaperFill) { fills += fill }
    }
}
