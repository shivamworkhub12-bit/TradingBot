package com.shivam.tradingbot.application.port.out

import com.shivam.tradingbot.domain.fno.IndexUnderlying
import com.shivam.tradingbot.domain.fno.OptionPaperFill
import com.shivam.tradingbot.domain.fno.OptionPaperPortfolio
import java.time.LocalDate

interface OptionPaperPortfolioStorePort {
    fun load(): OptionPaperPortfolio
    fun save(portfolio: OptionPaperPortfolio)
}

interface OptionPaperFillStorePort {
    fun save(fill: OptionPaperFill)

    /** Used by automation to keep each underlying's daily entry limit after a restart or manual exit. */
    fun hasFillOn(date: LocalDate, underlying: IndexUnderlying): Boolean
}
