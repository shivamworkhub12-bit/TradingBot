package com.shivam.tradingbot.application.usecase

import com.shivam.tradingbot.application.port.out.KiteCandleInterval
import com.shivam.tradingbot.application.port.out.KiteHistoricalDataPort
import com.shivam.tradingbot.application.port.out.KiteHistoricalDataRequest
import com.shivam.tradingbot.application.port.out.KiteOptionContractLookupPort
import com.shivam.tradingbot.application.port.out.KiteQuoteLookupPort
import com.shivam.tradingbot.application.port.out.OptionContractQuery
import com.shivam.tradingbot.application.port.out.OptionPaperFillStorePort
import com.shivam.tradingbot.application.port.out.OptionPaperPortfolioStorePort
import com.shivam.tradingbot.domain.fno.IndexUnderlying
import com.shivam.tradingbot.domain.fno.OptionContract
import com.shivam.tradingbot.domain.fno.OptionOrderIntent
import com.shivam.tradingbot.domain.fno.OptionType
import com.shivam.tradingbot.domain.model.Candle
import com.shivam.tradingbot.domain.model.OrderSide
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * An opt-in, paper-only automation loop. It never invokes Kite's order API.
 *
 * FIXED mode trades a contract supplied in .env. DYNAMIC_NIFTY_TREND chooses a
 * next-expiry ATM NIFTY CE or PE from the underlying's completed daily candles:
 * 5-day MA above 20-day MA selects CE; below selects PE; equal means no trade.
 * This is a learning rule, not a prediction or an investment recommendation.
 */
@Component
class FnoPaperAutomationService(
    private val portfolioStore: OptionPaperPortfolioStorePort,
    private val quoteLookup: KiteQuoteLookupPort,
    private val historicalData: KiteHistoricalDataPort,
    private val optionContracts: KiteOptionContractLookupPort,
    private val fillStore: OptionPaperFillStorePort,
    private val placeOptionPaperOrder: PlaceOptionPaperOrderUseCase,
    private val closeOptionPaperPosition: CloseOptionPaperPositionUseCase,
    @Value("\${FNO_PAPER_AUTOMATION_ENABLED:false}") private val enabled: Boolean,
    @Value("\${FNO_PAPER_AUTOMATION_MODE:FIXED}") private val mode: String,
    @Value("\${FNO_PAPER_AUTOMATION_TRADING_SYMBOL:}") private val tradingSymbol: String,
    @Value("\${FNO_PAPER_AUTOMATION_UNDERLYING:NIFTY}") private val underlying: IndexUnderlying,
    @Value("\${FNO_PAPER_AUTOMATION_EXPIRY:}") private val expiry: String,
    @Value("\${FNO_PAPER_AUTOMATION_STRIKE:0}") private val strike: BigDecimal,
    @Value("\${FNO_PAPER_AUTOMATION_OPTION_TYPE:PE}") private val optionType: OptionType,
    @Value("\${FNO_PAPER_AUTOMATION_LOT_SIZE:0}") private val lotSize: Int,
    @Value("\${FNO_PAPER_AUTOMATION_LOTS:1}") private val lots: Int,
    @Value("\${FNO_PAPER_AUTOMATION_ENTRY_PREMIUM:0}") private val entryPremium: BigDecimal,
    @Value("\${FNO_PAPER_AUTOMATION_MAX_OPTION_PREMIUM:250}") private val maximumOptionPremium: BigDecimal,
    @Value("\${FNO_PAPER_AUTOMATION_MIN_DAYS_TO_EXPIRY:7}") private val minimumDaysToExpiry: Long,
    @Value("\${FNO_PAPER_AUTOMATION_STOP_LOSS_PERCENT:25}") private val stopLossPercent: BigDecimal,
    @Value("\${FNO_PAPER_AUTOMATION_TARGET_PERCENT:25}") private val targetPercent: BigDecimal,
    private val clock: Clock = Clock.systemUTC(),
) {
    private var tradedOn: LocalDate? = null

    @Scheduled(fixedDelayString = "\${FNO_PAPER_AUTOMATION_POLL_DELAY_MS:60000}")
    fun poll() {
        if (!enabled || !isTradingDay()) return

        val today = istNow().toLocalDate()
        val time = istNow().toLocalTime()
        val portfolio = portfolioStore.load()
        // There is at most one paper position in this learning bot. Always manage it first.
        portfolio.positions.values.singleOrNull()?.let { position ->
            if (time in marketOpen..marketClose) {
                manageOpenPosition(
                    tradingSymbol = position.contract.tradingSymbol,
                    averagePremium = position.averagePremium,
                    today = today,
                    forceExit = time >= forcedExitTime,
                )
            }
            return
        }

        // New entries stop well before the mandatory paper exit at 3:25 PM IST.
        if (time !in marketOpen..lastEntryTime) return
        if (tradedOn == today || fillStore.hasFillOn(today)) {
            log.info("F&O paper automation will not open another position today")
            return
        }
        val candidate = when (mode.trim().uppercase()) {
            "FIXED" -> fixedCandidate()
            "DYNAMIC_NIFTY_TREND" -> dynamicNiftyCandidate(today)
            else -> {
                log.warn("F&O paper automation has an unknown mode '{}'; no action taken", mode)
                null
            }
        } ?: return

        val quote = runCatching { quoteLookup.latest("NFO:${candidate.contract.tradingSymbol}") }.getOrElse { error ->
            log.warn("F&O paper automation skipped: unable to get a quote for {} ({})", candidate.contract.tradingSymbol, error.message)
            return
        }
        if (quote.lastPrice > maximumOptionPremium) {
            log.info("F&O paper automation skipped {}: premium {} exceeds configured cap {}", candidate.contract.tradingSymbol, quote.lastPrice, maximumOptionPremium)
            return
        }
        if (candidate.minimumPremium != null && quote.lastPrice < candidate.minimumPremium) return

        val result = placeOptionPaperOrder.execute(
            OptionOrderIntent(candidate.contract, OrderSide.BUY, lots, quote.lastPrice),
            today,
        )
        if (result is OptionPaperOrderResult.Filled) {
            tradedOn = today
            log.info("Created F&O paper BUY for {} at {}; reason={}", candidate.contract.tradingSymbol, quote.lastPrice, candidate.reason)
        } else if (result is OptionPaperOrderResult.Rejected) {
            log.warn("F&O paper BUY rejected for {}: {}", candidate.contract.tradingSymbol, result.reason)
        }
    }

    private fun manageOpenPosition(
        tradingSymbol: String,
        averagePremium: BigDecimal,
        today: LocalDate,
        forceExit: Boolean,
    ) {
        val quote = runCatching { quoteLookup.latest("NFO:$tradingSymbol") }.getOrElse { error ->
            log.warn("F&O paper automation skipped: unable to get a quote for {} ({})", tradingSymbol, error.message)
            return
        }
        val stop = averagePremium.multiply(BigDecimal.ONE.subtract(stopLossPercent.movePointLeft(2)))
        val target = averagePremium.multiply(BigDecimal.ONE.add(targetPercent.movePointLeft(2)))
        if (forceExit || quote.lastPrice <= stop || quote.lastPrice >= target) {
            val result = closeOptionPaperPosition.execute(tradingSymbol, quote.lastPrice)
            if (result is OptionPaperOrderResult.Filled) {
                tradedOn = today
                val exit = when {
                    forceExit -> "3:25 PM forced exit"
                    quote.lastPrice <= stop -> "stop-loss"
                    else -> "target"
                }
                log.info("Closed F&O paper position {} at {} via {}", tradingSymbol, quote.lastPrice, exit)
            }
        }
    }

    private fun fixedCandidate(): Candidate? {
        if (tradingSymbol.isBlank() || expiry.isBlank() || strike <= BigDecimal.ZERO || lotSize <= 0 || lots <= 0 || entryPremium <= BigDecimal.ZERO) {
            log.warn("F&O paper automation is enabled but FIXED contract settings are incomplete; no action taken")
            return null
        }
        return Candidate(
            OptionContract(underlying, LocalDate.parse(expiry), strike, optionType, lotSize, tradingSymbol),
            entryPremium,
            "fixed contract configured in .env",
        )
    }

    private fun dynamicNiftyCandidate(today: LocalDate): Candidate? {
        if (lots != 1) {
            log.warn("DYNAMIC_NIFTY_TREND only permits one lot; configured lots={} so no action taken", lots)
            return null
        }
        val underlyingQuote = runCatching { quoteLookup.latest("NSE:NIFTY 50") }.getOrElse { error ->
            log.warn("Cannot choose a NIFTY paper contract: underlying quote unavailable ({})", error.message)
            return null
        }
        val candles = runCatching {
            historicalData.load(
                KiteHistoricalDataRequest(
                    symbol = "NSE:NIFTY 50",
                    instrumentToken = underlyingQuote.instrumentToken,
                    interval = KiteCandleInterval.DAY,
                    from = clock.instant().minusSeconds(90L * 24 * 60 * 60),
                    to = clock.instant(),
                ),
            )
        }.getOrElse { error ->
            log.warn("Cannot choose a NIFTY paper contract: daily candles unavailable ({})", error.message)
            return null
        }
        if (candles.size < 20) {
            log.warn("Cannot choose a NIFTY paper contract: only {} daily candles available", candles.size)
            return null
        }
        val shortAverage = averageClose(candles.takeLast(5))
        val longAverage = averageClose(candles.takeLast(20))
        val type = when {
            shortAverage > longAverage -> OptionType.CE
            shortAverage < longAverage -> OptionType.PE
            else -> {
                log.info("NIFTY trend is neutral (5-day MA={} and 20-day MA={}); no paper trade", shortAverage, longAverage)
                return null
            }
        }
        val expiry = optionContracts.availableExpiries(IndexUnderlying.NIFTY).firstOrNull { it >= today.plusDays(minimumDaysToExpiry) }
            ?: run {
                log.warn("Cannot choose a NIFTY paper contract: no expiry at least {} days away is available", minimumDaysToExpiry)
                return null
            }
        val strike = optionContracts.availableStrikes(IndexUnderlying.NIFTY, expiry, type)
            .minByOrNull { it.subtract(underlyingQuote.lastPrice).abs() }
            ?: run {
                log.warn("Cannot choose a NIFTY paper contract: no {} strikes are available for {}", type, expiry)
                return null
            }
        val contract = optionContracts.find(OptionContractQuery(IndexUnderlying.NIFTY, expiry, strike, type)).contract
        return Candidate(contract, null, "NIFTY 5-day MA=$shortAverage, 20-day MA=$longAverage; selected ATM $type at strike $strike")
    }

    private fun averageClose(candles: List<Candle>): BigDecimal = candles.map { it.close }.reduce(BigDecimal::add)
        .divide(BigDecimal(candles.size), 4, RoundingMode.HALF_UP)

    private fun isTradingDay(): Boolean {
        val now = istNow()
        return now.dayOfWeek !in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
    }

    private fun istNow() = clock.instant().atZone(ZoneId.of("Asia/Kolkata"))

    private data class Candidate(val contract: OptionContract, val minimumPremium: BigDecimal?, val reason: String)

    private companion object {
        val log = LoggerFactory.getLogger(FnoPaperAutomationService::class.java)
        val marketOpen: LocalTime = LocalTime.of(9, 15)
        val lastEntryTime: LocalTime = LocalTime.of(15, 0)
        val forcedExitTime: LocalTime = LocalTime.of(15, 25)
        val marketClose: LocalTime = LocalTime.of(15, 30)
    }
}
