package com.shivam.tradingbot.adapter.out.persistence

import com.shivam.tradingbot.application.port.out.PaperFillStorePort
import com.shivam.tradingbot.application.port.out.PaperFillHistoryPort
import com.shivam.tradingbot.application.port.out.PaperPortfolioStorePort
import com.shivam.tradingbot.domain.model.PaperFill
import com.shivam.tradingbot.domain.model.PaperPortfolio

class InMemoryPaperPortfolioStoreAdapter(initialPortfolio: PaperPortfolio) : PaperPortfolioStorePort {
    @Volatile
    private var portfolio = initialPortfolio

    override fun load(): PaperPortfolio = portfolio

    override fun save(portfolio: PaperPortfolio) {
        this.portfolio = portfolio
    }
}

class InMemoryPaperFillStoreAdapter : PaperFillStorePort, PaperFillHistoryPort {
    private val fills = mutableListOf<PaperFill>()

    override fun save(fill: PaperFill) {
        fills += fill
    }

    fun all(): List<PaperFill> = fills.toList()

    override fun latest(limit: Int): List<PaperFill> = fills.takeLast(limit).reversed()
}
