package com.shivam.tradingbot.application.usecase

import com.shivam.tradingbot.adapter.out.persistence.InMemoryOptionPaperFillStore
import com.shivam.tradingbot.adapter.out.persistence.InMemoryOptionPaperPortfolioStore
import com.shivam.tradingbot.adapter.out.persistence.InMemoryStrategyDecisionJournal
import com.shivam.tradingbot.application.port.out.KiteHistoricalDataPort
import com.shivam.tradingbot.application.port.out.KiteOptionContract
import com.shivam.tradingbot.application.port.out.KiteOptionContractLookupPort
import com.shivam.tradingbot.application.port.out.KiteQuote
import com.shivam.tradingbot.application.port.out.KiteQuoteLookupPort
import com.shivam.tradingbot.application.port.out.OptionContractQuery
import com.shivam.tradingbot.domain.fno.IndexUnderlying
import com.shivam.tradingbot.domain.fno.LongOptionRiskManager
import com.shivam.tradingbot.domain.fno.OptionContract
import com.shivam.tradingbot.domain.fno.OptionPaperPortfolio
import com.shivam.tradingbot.domain.fno.OptionType
import com.shivam.tradingbot.domain.model.Candle
import com.shivam.tradingbot.domain.strategy.EmaRsiIntradayStrategy
import com.shivam.tradingbot.domain.strategy.StrategyExecutionStatus
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals

class FnoPaperAutomationServiceTest {
    private val now = Instant.parse("2026-08-18T04:40:00Z")
    private val today = LocalDate.of(2026, 8, 18)
    private val expiry = LocalDate.of(2026, 8, 27)

    @Test
    fun `dynamic index mode opens independent NIFTY and BANKNIFTY paper positions`() {
        val portfolioStore = InMemoryOptionPaperPortfolioStore(OptionPaperPortfolio(BigDecimal("100000")))
        val fillStore = InMemoryOptionPaperFillStore()
        val journal = InMemoryStrategyDecisionJournal()
        val service = service(portfolioStore, fillStore, journal)

        service.poll()
        service.poll()

        val portfolio = portfolioStore.load()
        assertEquals(setOf(IndexUnderlying.NIFTY, IndexUnderlying.BANKNIFTY), portfolio.positions.values.map { it.contract.underlying }.toSet())
        assertEquals(2, fillStore.fills.size)
        assertEquals(
            StrategyExecutionStatus.PAPER_ORDER_FILLED,
            journal.latest(10, "NSE:NIFTY 50").single().executionStatus,
        )
        assertEquals(
            StrategyExecutionStatus.PAPER_ORDER_FILLED,
            journal.latest(10, "NSE:NIFTY BANK").single().executionStatus,
        )
    }

    @Test
    fun `forced exit closes both index positions`() {
        val portfolioStore = InMemoryOptionPaperPortfolioStore(OptionPaperPortfolio(BigDecimal("100000")))
        val fillStore = InMemoryOptionPaperFillStore()
        val journal = InMemoryStrategyDecisionJournal()

        service(portfolioStore, fillStore, journal).poll()
        service(
            portfolioStore,
            fillStore,
            journal,
            clock = Clock.fixed(Instant.parse("2026-08-18T09:55:00Z"), ZoneOffset.UTC),
        ).poll()

        assertEquals(emptyMap(), portfolioStore.load().positions)
        assertEquals(4, fillStore.fills.size)
    }

    private fun service(
        portfolioStore: InMemoryOptionPaperPortfolioStore,
        fillStore: InMemoryOptionPaperFillStore,
        journal: InMemoryStrategyDecisionJournal,
        clock: Clock = Clock.fixed(now, ZoneOffset.UTC),
    ): FnoPaperAutomationService {
        val quotes = mapOf(
            "NSE:NIFTY 50" to KiteQuote("NSE:NIFTY 50", 1L, BigDecimal("24210")),
            "NSE:NIFTY BANK" to KiteQuote("NSE:NIFTY BANK", 2L, BigDecimal("55100")),
            "NFO:NIFTY26AUG24200CE" to KiteQuote("NFO:NIFTY26AUG24200CE", 3L, BigDecimal("100")),
            "NFO:BANKNIFTY26AUG55100CE" to KiteQuote("NFO:BANKNIFTY26AUG55100CE", 4L, BigDecimal("100")),
        )
        val quoteLookup = KiteQuoteLookupPort { symbol -> requireNotNull(quotes[symbol]) { "Unexpected quote: $symbol" } }
        val historicalData = KiteHistoricalDataPort { request ->
            when (request.symbol) {
                "NSE:NIFTY 50" -> bullishCandles(request.symbol, BigDecimal("24200"), BigDecimal("24400"))
                "NSE:NIFTY BANK" -> bullishCandles(request.symbol, BigDecimal("55000"), BigDecimal("55500"))
                else -> error("Unexpected history request: ${request.symbol}")
            }
        }
        val optionContracts = object : KiteOptionContractLookupPort {
            override fun find(query: OptionContractQuery): KiteOptionContract {
                val symbol = when (query.underlying) {
                    IndexUnderlying.NIFTY -> "NIFTY26AUG24200CE"
                    IndexUnderlying.BANKNIFTY -> "BANKNIFTY26AUG55100CE"
                }
                val lotSize = if (query.underlying == IndexUnderlying.NIFTY) 65 else 30
                return KiteOptionContract(
                    OptionContract(query.underlying, query.expiry, query.strike, query.optionType, lotSize, symbol),
                    instrumentToken = 10L,
                )
            }

            override fun availableExpiries(underlying: IndexUnderlying) = listOf(expiry)

            override fun availableStrikes(
                underlying: IndexUnderlying,
                expiry: LocalDate,
                optionType: OptionType,
            ) = when (underlying) {
                IndexUnderlying.NIFTY -> listOf(BigDecimal("24200"))
                IndexUnderlying.BANKNIFTY -> listOf(BigDecimal("55100"))
            }
        }
        return FnoPaperAutomationService(
            portfolioStore = portfolioStore,
            quoteLookup = quoteLookup,
            historicalData = historicalData,
            optionContracts = optionContracts,
            fillStore = fillStore,
            decisionJournal = journal,
            intradayStrategy = EmaRsiIntradayStrategy(bullishRsiRange = BigDecimal.ZERO..BigDecimal("100")),
            placeOptionPaperOrder = PlaceOptionPaperOrderUseCase(portfolioStore, fillStore, LongOptionRiskManager()),
            closeOptionPaperPosition = CloseOptionPaperPositionUseCase(portfolioStore, fillStore),
            enabled = true,
            mode = "DYNAMIC_INDEX_TREND",
            tradingSymbol = "",
            underlying = IndexUnderlying.NIFTY,
            expiry = "",
            strike = BigDecimal.ZERO,
            optionType = OptionType.PE,
            lotSize = 0,
            lots = 1,
            entryPremium = BigDecimal.ZERO,
            defaultMaximumOptionPremium = BigDecimal("250"),
            minimumDaysToExpiry = 7,
            lastEntryTimeText = "14:30",
            dynamicUnderlyingText = "NIFTY,BANKNIFTY",
            niftyMaximumOptionPremium = BigDecimal("250"),
            bankNiftyMaximumOptionPremium = BigDecimal("250"),
            niftyStopLossPercent = BigDecimal("25"),
            bankNiftyStopLossPercent = BigDecimal("25"),
            niftyTargetPercent = BigDecimal("25"),
            bankNiftyTargetPercent = BigDecimal("25"),
            clock = clock,
        )
    }

    private fun bullishCandles(symbol: String, base: BigDecimal, breakout: BigDecimal): List<Candle> =
        (List(23) { base } + breakout).mapIndexed { index, price ->
            Candle(
                symbol = symbol,
                closedAt = Instant.parse("2026-08-18T02:35:00Z").plusSeconds(index * 300L),
                open = price,
                high = price,
                low = price,
                close = price,
                volume = 1_000,
            )
        }
}
