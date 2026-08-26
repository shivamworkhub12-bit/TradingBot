package com.shivam.tradingbot.domain.strategy

import com.shivam.tradingbot.domain.model.Candle
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

enum class IntradayDirection { BULLISH, BEARISH, NEUTRAL }

data class IntradayStrategyDecision(
    val direction: IntradayDirection,
    val fastEma: BigDecimal?,
    val slowEma: BigDecimal?,
    val rsi: BigDecimal?,
    val reason: String,
)

/**
 * Pure intraday direction rule. It evaluates completed candles only; deciding
 * which candles are complete belongs to the application layer. A trade needs
 * momentum, a fresh price breakout, and enough realized range to avoid taking
 * tiny crossovers in an inactive market.
 */
class EmaRsiIntradayStrategy(
    private val fastPeriod: Int = 9,
    private val slowPeriod: Int = 21,
    private val rsiPeriod: Int = 14,
    private val bullishRsiRange: ClosedRange<BigDecimal> = BigDecimal("40")..BigDecimal("70"),
    private val bearishRsiRange: ClosedRange<BigDecimal> = BigDecimal("30")..BigDecimal("60"),
    private val confirmationCandles: Int = 8,
    private val minimumEmaSeparationBasisPoints: BigDecimal = BigDecimal.ONE,
    private val breakoutPeriod: Int = 20,
    private val atrPeriod: Int = 14,
    private val minimumAtrBasisPoints: BigDecimal = BigDecimal("2"),
) {
    init {
        require(fastPeriod > 0) { "fastPeriod must be positive" }
        require(slowPeriod > fastPeriod) { "slowPeriod must be greater than fastPeriod" }
        require(rsiPeriod > 0) { "rsiPeriod must be positive" }
        require(confirmationCandles > 0) { "confirmationCandles must be positive" }
        require(minimumEmaSeparationBasisPoints >= BigDecimal.ZERO) {
            "minimumEmaSeparationBasisPoints must not be negative"
        }
        require(breakoutPeriod > 0) { "breakoutPeriod must be positive" }
        require(atrPeriod > 0) { "atrPeriod must be positive" }
        require(minimumAtrBasisPoints >= BigDecimal.ZERO) { "minimumAtrBasisPoints must not be negative" }
    }

    fun evaluate(candles: List<Candle>): IntradayStrategyDecision {
        val requiredCandles = maxOf(slowPeriod + 2, rsiPeriod + 1, breakoutPeriod + 1, atrPeriod + 1)
        if (candles.size < requiredCandles) return neutral("Not enough completed candles")
        require(candles.map { it.symbol }.distinct().size == 1) { "all candles must use one symbol" }
        require(candles.zipWithNext().all { (left, right) -> left.closedAt < right.closedAt }) {
            "candles must be ordered oldest to newest"
        }

        val closes = candles.map { it.close }
        val fast = ema(closes, fastPeriod)
        val slow = ema(closes, slowPeriod)
        val currentRsi = rsi(closes.takeLast(rsiPeriod + 1))
        val currentFast = fast.last()
        val currentSlow = slow.last()
        val recentTransitions = (fast.lastIndex - confirmationCandles + 1).coerceAtLeast(1)..fast.lastIndex
        val bullishCrossedRecently = recentTransitions.any { index ->
            fast[index - 1] <= slow[index - 1] && fast[index] > slow[index]
        }
        val bearishCrossedRecently = recentTransitions.any { index ->
            fast[index - 1] >= slow[index - 1] && fast[index] < slow[index]
        }
        val separationBasisPoints = currentFast.subtract(currentSlow).abs()
            .divide(closes.last(), mathContext)
            .multiply(basisPoints, mathContext)
        val breakoutWindow = candles.dropLast(1).takeLast(breakoutPeriod)
        val previousHigh = breakoutWindow.maxOf { it.high }
        val previousLow = breakoutWindow.minOf { it.low }
        val bullishBreakout = candles.last().close > previousHigh
        val bearishBreakout = candles.last().close < previousLow
        val atrBasisPoints = averageTrueRange(candles.takeLast(atrPeriod + 1))
            .divide(closes.last(), mathContext)
            .multiply(basisPoints, mathContext)
        val activeVolatilityRegime = atrBasisPoints >= minimumAtrBasisPoints
        val trend = when {
            currentFast > currentSlow -> IntradayDirection.BULLISH
            currentFast < currentSlow -> IntradayDirection.BEARISH
            else -> IntradayDirection.NEUTRAL
        }
        val crossover = when {
            bullishCrossedRecently -> "RECENT_BULLISH"
            bearishCrossedRecently -> "RECENT_BEARISH"
            else -> "NONE"
        }
        val rsiAllowed = when (trend) {
            IntradayDirection.BULLISH -> currentRsi in bullishRsiRange
            IntradayDirection.BEARISH -> currentRsi in bearishRsiRange
            IntradayDirection.NEUTRAL -> false
        }

        val direction = when {
            bullishCrossedRecently &&
                currentFast > currentSlow &&
                separationBasisPoints >= minimumEmaSeparationBasisPoints &&
                bullishBreakout &&
                activeVolatilityRegime &&
                currentRsi in bullishRsiRange ->
                IntradayDirection.BULLISH
            bearishCrossedRecently &&
                currentFast < currentSlow &&
                separationBasisPoints >= minimumEmaSeparationBasisPoints &&
                bearishBreakout &&
                activeVolatilityRegime &&
                currentRsi in bearishRsiRange ->
                IntradayDirection.BEARISH
            else -> IntradayDirection.NEUTRAL
        }
        val breakout = when {
            bullishBreakout -> "ABOVE_${breakoutPeriod}_HIGH"
            bearishBreakout -> "BELOW_${breakoutPeriod}_LOW"
            else -> "NONE"
        }
        val reason = "EMA($fastPeriod)=${currentFast.display()}, EMA($slowPeriod)=${currentSlow.display()}, " +
            "trend=$trend, crossover=$crossover, " +
            "separation=${separationBasisPoints.display(4)} bps(${gate(separationBasisPoints >= minimumEmaSeparationBasisPoints)}), " +
            "RSI($rsiPeriod)=${currentRsi.display()}(${gate(rsiAllowed)}), breakout=$breakout, " +
            "ATR($atrPeriod)=${atrBasisPoints.display(4)} bps(${gate(activeVolatilityRegime)})"
        return IntradayStrategyDecision(direction, currentFast, currentSlow, currentRsi, reason)
    }

    private fun ema(values: List<BigDecimal>, period: Int): List<BigDecimal> {
        val multiplier = BigDecimal(2).divide(BigDecimal(period + 1), mathContext)
        return values.drop(1).runningFold(values.first()) { previous, value ->
            value.subtract(previous).multiply(multiplier, mathContext).add(previous, mathContext)
        }
    }

    private fun rsi(values: List<BigDecimal>): BigDecimal {
        val changes = values.zipWithNext { previous, current -> current.subtract(previous) }
        val gains = changes.map { it.max(BigDecimal.ZERO) }.reduce(BigDecimal::add)
        val losses = changes.map { it.min(BigDecimal.ZERO).abs() }.reduce(BigDecimal::add)
        if (losses.compareTo(BigDecimal.ZERO) == 0) return BigDecimal("100")
        if (gains.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO
        val relativeStrength = gains.divide(losses, mathContext)
        return BigDecimal("100").subtract(
            BigDecimal("100").divide(BigDecimal.ONE.add(relativeStrength), mathContext),
        )
    }

    private fun averageTrueRange(candles: List<Candle>): BigDecimal {
        val trueRanges = candles.zipWithNext { previous, current ->
            maxOf(
                current.high.subtract(current.low),
                current.high.subtract(previous.close).abs(),
                current.low.subtract(previous.close).abs(),
            )
        }
        return trueRanges.reduce(BigDecimal::add).divide(BigDecimal(trueRanges.size), mathContext)
    }

    private fun neutral(reason: String) = IntradayStrategyDecision(
        IntradayDirection.NEUTRAL,
        null,
        null,
        null,
        reason,
    )

    private fun BigDecimal.display(scale: Int = 2): BigDecimal = setScale(scale, RoundingMode.HALF_UP)

    private fun gate(passed: Boolean): String = if (passed) "PASS" else "FAIL"

    private companion object {
        val mathContext = MathContext(12, RoundingMode.HALF_UP)
        val basisPoints = BigDecimal("10000")
    }
}
