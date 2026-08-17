package com.shivam.tradingbot.adapter.`in`.web

import com.shivam.tradingbot.application.usecase.GetOptionPaperPortfolioMtmUseCase
import com.shivam.tradingbot.application.usecase.OptionPaperPortfolioMtm
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** Read-only mark-to-market valuation of local F&O paper positions. */
@RestController
@RequestMapping("/fno/paper-portfolio")
class OptionPaperMtmController(private val getOptionPaperPortfolioMtm: GetOptionPaperPortfolioMtmUseCase) {
    @GetMapping("/mark-to-market")
    fun markToMarket(): OptionPaperPortfolioMtm = getOptionPaperPortfolioMtm.execute()
}
