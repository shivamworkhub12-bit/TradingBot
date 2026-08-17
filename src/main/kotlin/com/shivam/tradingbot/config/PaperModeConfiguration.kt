package com.shivam.tradingbot.config

import com.shivam.tradingbot.adapter.out.persistence.InMemoryPaperFillStoreAdapter
import com.shivam.tradingbot.adapter.out.persistence.InMemoryPaperPortfolioStoreAdapter
import com.shivam.tradingbot.adapter.out.persistence.InMemorySignalStoreAdapter
import com.shivam.tradingbot.application.port.out.PaperPortfolioStorePort
import com.shivam.tradingbot.application.port.out.SignalStorePort
import com.shivam.tradingbot.domain.model.PaperPortfolio
import com.shivam.tradingbot.adapter.out.persistence.InMemoryOptionPaperFillStore
import com.shivam.tradingbot.adapter.out.persistence.InMemoryOptionPaperPortfolioStore
import com.shivam.tradingbot.domain.fno.OptionPaperPortfolio
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.math.BigDecimal

/** Fast, isolated persistence used only by the test-oriented paper profile. */
@Configuration
@Profile("paper")
class PaperModeConfiguration {
    @Bean
    fun paperPortfolioStore(): PaperPortfolioStorePort = InMemoryPaperPortfolioStoreAdapter(
        PaperPortfolio(availableCash = BigDecimal("100000"), dailyRealizedProfitLoss = BigDecimal.ZERO),
    )

    @Bean
    fun paperFillStore(): InMemoryPaperFillStoreAdapter = InMemoryPaperFillStoreAdapter()

    @Bean
    fun signalStore(): InMemorySignalStoreAdapter = InMemorySignalStoreAdapter()

    @Bean
    fun optionPaperPortfolioStore() = InMemoryOptionPaperPortfolioStore(OptionPaperPortfolio(BigDecimal("100000")))

    @Bean
    fun optionPaperFillStore() = InMemoryOptionPaperFillStore()
}
