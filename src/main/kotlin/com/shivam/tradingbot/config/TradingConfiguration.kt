package com.shivam.tradingbot.config

import com.shivam.tradingbot.adapter.out.marketdata.SampleNseMarketDataAdapter
import com.shivam.tradingbot.adapter.out.paper.InMemoryPaperBrokerAdapter
import com.shivam.tradingbot.application.port.out.MarketDataPort
import com.shivam.tradingbot.adapter.out.kite.KiteConnectClient
import com.shivam.tradingbot.application.port.out.PaperBrokerPort
import com.shivam.tradingbot.application.port.out.PaperFillStorePort
import com.shivam.tradingbot.application.port.out.PaperPortfolioStorePort
import com.shivam.tradingbot.application.port.out.SignalStorePort
import com.shivam.tradingbot.application.usecase.AssessOrderRiskUseCase
import com.shivam.tradingbot.application.usecase.EvaluateStrategyUseCase
import com.shivam.tradingbot.application.usecase.PlacePaperOrderUseCase
import com.shivam.tradingbot.application.usecase.RunBacktestUseCase
import com.shivam.tradingbot.application.usecase.StartKiteLoginUseCase
import com.shivam.tradingbot.application.usecase.CompleteKiteLoginUseCase
import com.shivam.tradingbot.application.usecase.FetchKiteHistoricalCandlesUseCase
import com.shivam.tradingbot.application.usecase.FindKiteInstrumentUseCase
import com.shivam.tradingbot.application.usecase.RunKiteBacktestUseCase
import com.shivam.tradingbot.application.usecase.FindKiteOptionContractUseCase
import com.shivam.tradingbot.application.usecase.GetKiteQuoteUseCase
import com.shivam.tradingbot.domain.risk.FixedLimitRiskManager
import com.shivam.tradingbot.domain.risk.RiskManager
import com.shivam.tradingbot.domain.strategy.MovingAverageCrossoverStrategy
import com.shivam.tradingbot.domain.strategy.EmaRsiIntradayStrategy
import com.shivam.tradingbot.domain.strategy.TradingStrategy
import com.shivam.tradingbot.domain.cost.NseEquityDeliveryCostCalculator
import com.shivam.tradingbot.domain.cost.FixedBpsSlippage
import com.shivam.tradingbot.domain.fno.LongOptionRiskManager
import com.shivam.tradingbot.application.port.out.OptionPaperFillStorePort
import com.shivam.tradingbot.application.port.out.OptionPaperPortfolioStorePort
import com.shivam.tradingbot.application.usecase.PlaceOptionPaperOrderUseCase
import com.shivam.tradingbot.application.usecase.GetOptionPaperPortfolioMtmUseCase
import com.shivam.tradingbot.application.usecase.CloseOptionPaperPositionUseCase
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.math.BigDecimal

/** The outermost layer chooses concrete implementations for the application. */
@Configuration
class TradingConfiguration {
    @Bean
    fun marketData(): MarketDataPort = SampleNseMarketDataAdapter()

    @Bean
    fun strategy(): TradingStrategy = MovingAverageCrossoverStrategy(shortWindow = 3, longWindow = 10)

    @Bean
    fun emaRsiIntradayStrategy(
        @Value("\${FNO_PAPER_AUTOMATION_CONFIRMATION_CANDLES:8}") confirmationCandles: Int,
        @Value("\${FNO_PAPER_AUTOMATION_MIN_EMA_SEPARATION_BPS:1}") minimumEmaSeparationBasisPoints: BigDecimal,
        @Value("\${FNO_PAPER_AUTOMATION_BREAKOUT_PERIOD:20}") breakoutPeriod: Int,
        @Value("\${FNO_PAPER_AUTOMATION_ATR_PERIOD:14}") atrPeriod: Int,
        @Value("\${FNO_PAPER_AUTOMATION_MIN_ATR_BPS:2}") minimumAtrBasisPoints: BigDecimal,
    ) = EmaRsiIntradayStrategy(
        confirmationCandles = confirmationCandles,
        minimumEmaSeparationBasisPoints = minimumEmaSeparationBasisPoints,
        breakoutPeriod = breakoutPeriod,
        atrPeriod = atrPeriod,
        minimumAtrBasisPoints = minimumAtrBasisPoints,
    )

    @Bean
    fun riskManager(): RiskManager = FixedLimitRiskManager(
        maximumOrderValue = java.math.BigDecimal("10000"),
        maximumDailyLoss = java.math.BigDecimal("500"),
    )

    @Bean
    fun longOptionRiskManager() = LongOptionRiskManager()

    @Bean
    fun placeOptionPaperOrderUseCase(
        optionPaperPortfolioStore: OptionPaperPortfolioStorePort,
        optionPaperFillStore: OptionPaperFillStorePort,
        longOptionRiskManager: LongOptionRiskManager,
    ) = PlaceOptionPaperOrderUseCase(optionPaperPortfolioStore, optionPaperFillStore, longOptionRiskManager)

    @Bean
    fun getOptionPaperPortfolioMtmUseCase(
        optionPaperPortfolioStore: OptionPaperPortfolioStorePort,
        kiteConnectClient: KiteConnectClient,
    ) = GetOptionPaperPortfolioMtmUseCase(optionPaperPortfolioStore, kiteConnectClient)

    @Bean
    fun closeOptionPaperPositionUseCase(
        optionPaperPortfolioStore: OptionPaperPortfolioStorePort,
        optionPaperFillStore: OptionPaperFillStorePort,
    ) = CloseOptionPaperPositionUseCase(optionPaperPortfolioStore, optionPaperFillStore)

    @Bean
    fun assessOrderRiskUseCase(riskManager: RiskManager) = AssessOrderRiskUseCase(riskManager)

    @Bean
    fun paperBroker(): PaperBrokerPort = InMemoryPaperBrokerAdapter()

    @Bean
    fun placePaperOrderUseCase(
        portfolioStore: PaperPortfolioStorePort,
        paperBroker: PaperBrokerPort,
        fillStore: PaperFillStorePort,
        riskManager: RiskManager,
    ) = PlacePaperOrderUseCase(portfolioStore, paperBroker, fillStore, riskManager)

    @Bean
    fun evaluateStrategyUseCase(
        marketData: MarketDataPort,
        signalStore: SignalStorePort,
        strategy: TradingStrategy,
    ) = EvaluateStrategyUseCase(
        marketData = marketData,
        signalStore = signalStore,
        strategy = strategy,
        candleHistorySize = 11,
    )

    @Bean
    fun runBacktestUseCase(
        marketData: MarketDataPort,
        strategy: TradingStrategy,
        riskManager: RiskManager,
    ) = RunBacktestUseCase(
        marketData,
        strategy,
        riskManager,
        NseEquityDeliveryCostCalculator,
        FixedBpsSlippage(basisPoints = 5),
    )

    @Bean
    fun startKiteLoginUseCase(kiteConnectClient: KiteConnectClient) = StartKiteLoginUseCase(kiteConnectClient)

    @Bean
    fun completeKiteLoginUseCase(kiteConnectClient: KiteConnectClient) = CompleteKiteLoginUseCase(kiteConnectClient)

    @Bean
    fun fetchKiteHistoricalCandlesUseCase(kiteConnectClient: KiteConnectClient) =
        FetchKiteHistoricalCandlesUseCase(kiteConnectClient)

    @Bean
    fun findKiteInstrumentUseCase(kiteConnectClient: KiteConnectClient) = FindKiteInstrumentUseCase(kiteConnectClient)

    @Bean
    fun findKiteOptionContractUseCase(kiteConnectClient: KiteConnectClient) = FindKiteOptionContractUseCase(kiteConnectClient)

    @Bean
    fun getKiteQuoteUseCase(kiteConnectClient: KiteConnectClient) = GetKiteQuoteUseCase(kiteConnectClient)

    @Bean
    fun runKiteBacktestUseCase(
        kiteConnectClient: KiteConnectClient,
        runBacktestUseCase: RunBacktestUseCase,
    ) = RunKiteBacktestUseCase(kiteConnectClient, runBacktestUseCase)
}
