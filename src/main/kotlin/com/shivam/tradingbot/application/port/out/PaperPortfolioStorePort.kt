package com.shivam.tradingbot.application.port.out

import com.shivam.tradingbot.domain.model.PaperPortfolio

interface PaperPortfolioStorePort {
    fun load(): PaperPortfolio
    fun save(portfolio: PaperPortfolio)
}
