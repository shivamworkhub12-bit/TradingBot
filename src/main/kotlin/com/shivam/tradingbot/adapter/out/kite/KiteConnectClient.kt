package com.shivam.tradingbot.adapter.out.kite

import tools.jackson.databind.JsonNode
import com.shivam.tradingbot.application.port.out.KiteAuthenticationPort
import com.shivam.tradingbot.application.port.out.KiteCandleInterval
import com.shivam.tradingbot.application.port.out.KiteHistoricalDataPort
import com.shivam.tradingbot.application.port.out.KiteHistoricalDataRequest
import com.shivam.tradingbot.application.port.out.KiteInstrument
import com.shivam.tradingbot.application.port.out.KiteInstrumentLookupPort
import com.shivam.tradingbot.application.port.out.KiteSession
import com.shivam.tradingbot.application.port.out.KiteOptionContract
import com.shivam.tradingbot.application.port.out.KiteOptionContractLookupPort
import com.shivam.tradingbot.application.port.out.OptionContractQuery
import com.shivam.tradingbot.application.port.out.KiteQuote
import com.shivam.tradingbot.application.port.out.KiteQuoteLookupPort
import com.shivam.tradingbot.domain.fno.OptionContract
import com.shivam.tradingbot.domain.fno.OptionType
import com.shivam.tradingbot.domain.model.Candle
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import java.math.BigDecimal
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.GZIPInputStream

/** Outer adapter for Kite Connect. The access token remains only in server memory. */
class KiteConnectClient(
    private val apiKey: String,
    private val apiSecret: String,
    private val restClient: RestClient = RestClient.create("https://api.kite.trade"),
    private val clock: Clock = Clock.systemUTC(),
) : KiteAuthenticationPort, KiteHistoricalDataPort, KiteInstrumentLookupPort, KiteOptionContractLookupPort, KiteQuoteLookupPort {
    @Volatile
    private var accessToken: String? = null

    @Volatile
    private var nfoContracts: Pair<java.time.LocalDate, List<NfoInstrumentRow>>? = null

    override fun loginUrl(): String {
        requireConfigured()
        return "https://kite.zerodha.com/connect/login?v=3&api_key=$apiKey"
    }

    override fun completeLogin(requestToken: String): KiteSession {
        requireConfigured()
        val form = LinkedMultiValueMap<String, String>().apply {
            add("api_key", apiKey)
            add("request_token", requestToken)
            add("checksum", sha256(apiKey + requestToken + apiSecret))
        }
        val response = restClient.post()
            .uri("/session/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(JsonNode::class.java) ?: error("Kite returned an empty login response")

        accessToken = response.path("data").path("access_token").asString().ifBlank {
            error("Kite login response did not contain an access token")
        }
        return KiteSession(expiresAt = clock.instant().atZone(indiaZone).toLocalDate()
            .plusDays(1)
            .atTime(6, 0)
            .atZone(indiaZone)
            .toInstant())
    }

    override fun load(request: KiteHistoricalDataRequest): List<Candle> {
        requireConfigured()
        val token = requireNotNull(accessToken) { "Authenticate with Kite before requesting historical candles" }
        val response = restClient.get()
            .uri { builder ->
                builder.path("/instruments/historical/{instrumentToken}/{interval}")
                    .queryParam("from", kiteDateTime(request.from))
                    .queryParam("to", kiteDateTime(request.to))
                    .build(request.instrumentToken, request.interval.apiValue)
            }
            .header("X-Kite-Version", "3")
            .header(HttpHeaders.AUTHORIZATION, "token $apiKey:$token")
            .retrieve()
            .body(JsonNode::class.java) ?: error("Kite returned an empty historical-data response")

        return response.path("data").path("candles").iterator().asSequence()
            .map { candle -> candle.toCandle(request.symbol) }
            .toList()
    }

    override fun find(symbol: String): KiteInstrument {
        val quote = latest(symbol)
        return KiteInstrument(quote.symbol, quote.instrumentToken)
    }

    override fun latest(symbol: String): KiteQuote {
        requireConfigured()
        val token = requireNotNull(accessToken) { "Authenticate with Kite before looking up an instrument" }
        val response = restClient.get()
            .uri { builder -> builder.path("/quote/ltp").queryParam("i", symbol).build() }
            .header("X-Kite-Version", "3")
            .header(HttpHeaders.AUTHORIZATION, "token $apiKey:$token")
            .retrieve()
            .body(JsonNode::class.java) ?: error("Kite returned an empty instrument response")
        val instrumentToken = response.path("data").path(symbol).path("instrument_token").asLong()
        check(instrumentToken > 0) { "Kite did not find an instrument for $symbol" }
        val lastPrice = response.path("data").path(symbol).path("last_price").decimalValue()
        check(lastPrice > BigDecimal.ZERO) { "Kite did not return a price for $symbol" }
        return KiteQuote(symbol, instrumentToken, lastPrice)
    }

    override fun find(query: OptionContractQuery): KiteOptionContract {
        val match = loadNfoInstruments().singleOrNull { row ->
            row.expiry == query.expiry &&
                row.strike.compareTo(query.strike) == 0 &&
                row.optionType == query.optionType &&
                row.tradingSymbol.startsWith(query.underlying.name)
        } ?: error("Kite did not find the requested ${query.underlying} option contract")

        return KiteOptionContract(
            contract = OptionContract(
                underlying = query.underlying,
                expiry = match.expiry,
                strike = match.strike,
                optionType = match.optionType,
                lotSize = match.lotSize,
                tradingSymbol = match.tradingSymbol,
            ),
            instrumentToken = match.instrumentToken,
        )
    }

    override fun availableExpiries(underlying: com.shivam.tradingbot.domain.fno.IndexUnderlying): List<java.time.LocalDate> =
        loadNfoInstruments()
            .filter { it.tradingSymbol.startsWith(underlying.name) }
            .map { it.expiry }
            .distinct()
            .sorted()

    override fun availableStrikes(
        underlying: com.shivam.tradingbot.domain.fno.IndexUnderlying,
        expiry: java.time.LocalDate,
        optionType: OptionType,
    ): List<BigDecimal> = loadNfoInstruments()
        .filter {
            it.tradingSymbol.startsWith(underlying.name) &&
                it.expiry == expiry &&
                it.optionType == optionType
        }
        .map { it.strike }
        .distinct()
        .sorted()

    private fun loadNfoInstruments(): List<NfoInstrumentRow> {
        val today = clock.instant().atZone(indiaZone).toLocalDate()
        nfoContracts?.takeIf { it.first == today }?.let { return it.second }
        synchronized(this) {
            nfoContracts?.takeIf { it.first == today }?.let { return it.second }
            requireConfigured()
            val token = requireNotNull(accessToken) { "Authenticate with Kite before looking up an option contract" }
            val body = restClient.get()
                .uri("/instruments/NFO")
                .header("X-Kite-Version", "3")
                .header(HttpHeaders.AUTHORIZATION, "token $apiKey:$token")
                .retrieve()
                .body(ByteArray::class.java) ?: error("Kite returned an empty NFO instrument list")
            val rows = decodeCsv(body).lineSequence().drop(1).mapNotNull(::parseNfoRow).toList()
            nfoContracts = today to rows
            return rows
        }
    }

    private fun parseNfoRow(line: String): NfoInstrumentRow? {
        val values = line.split(',')
        if (values.size < 12 || values[9] !in setOf("CE", "PE")) return null
        return NfoInstrumentRow(
            instrumentToken = values[0].toLong(),
            tradingSymbol = values[2],
            expiry = java.time.LocalDate.parse(values[5]),
            strike = BigDecimal(values[6]),
            lotSize = values[8].toInt(),
            optionType = OptionType.valueOf(values[9]),
        )
    }

    private fun decodeCsv(body: ByteArray): String =
        if (body.size >= 2 && body[0] == 0x1f.toByte() && body[1] == 0x8b.toByte()) {
            GZIPInputStream(body.inputStream()).bufferedReader().use { it.readText() }
        } else {
            body.toString(Charsets.UTF_8)
        }

    private fun JsonNode.toCandle(symbol: String) = Candle(
        symbol = symbol,
        // Kite returns offsets as +0530 (without the colon), while the default
        // ISO parser expects +05:30.
        closedAt = OffsetDateTime.parse(this[0].asString(), kiteTimestampFormat).toInstant(),
        open = BigDecimal(this[1].asString()),
        high = BigDecimal(this[2].asString()),
        low = BigDecimal(this[3].asString()),
        close = BigDecimal(this[4].asString()),
        volume = this[5].asLong(),
    )

    private fun requireConfigured() {
        check(apiKey.isNotBlank()) { "KITE_API_KEY is not configured" }
        check(apiSecret.isNotBlank()) { "KITE_API_SECRET is not configured" }
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray())
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private companion object {
        val indiaZone: ZoneId = ZoneId.of("Asia/Kolkata")
        val kiteDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val kiteTimestampFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ")
    }

    private fun kiteDateTime(instant: Instant): String = kiteDateFormat.format(instant.atZone(indiaZone))

    private data class NfoInstrumentRow(
        val instrumentToken: Long,
        val tradingSymbol: String,
        val expiry: java.time.LocalDate,
        val strike: BigDecimal,
        val lotSize: Int,
        val optionType: OptionType,
    )
}
