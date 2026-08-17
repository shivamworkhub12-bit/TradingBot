package com.shivam.tradingbot.adapter.`in`.web

import com.shivam.tradingbot.application.port.out.KiteOptionContract
import com.shivam.tradingbot.application.port.out.OptionContractQuery
import com.shivam.tradingbot.application.usecase.FindKiteOptionContractUseCase
import com.shivam.tradingbot.domain.fno.IndexUnderlying
import com.shivam.tradingbot.domain.fno.OptionType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.LocalDate

/** Reads a current option contract from Kite's NFO instrument master. */
@RestController
@RequestMapping("/kite/fno/contracts")
class KiteFnoContractController(
    private val findKiteOptionContract: FindKiteOptionContractUseCase,
) {
    @GetMapping("/lookup")
    fun lookup(
        @RequestParam underlying: IndexUnderlying,
        @RequestParam expiry: LocalDate,
        @RequestParam strike: BigDecimal,
        @RequestParam optionType: OptionType,
    ): KiteFnoContractResponse = KiteFnoContractResponse.from(
        findKiteOptionContract.execute(OptionContractQuery(underlying, expiry, strike, optionType)),
    )

    @GetMapping("/expiries")
    fun expiries(@RequestParam underlying: IndexUnderlying): List<LocalDate> =
        findKiteOptionContract.availableExpiries(underlying)

    @GetMapping("/strikes")
    fun strikes(
        @RequestParam underlying: IndexUnderlying,
        @RequestParam expiry: LocalDate,
        @RequestParam optionType: OptionType,
    ): List<BigDecimal> = findKiteOptionContract.availableStrikes(underlying, expiry, optionType)
}

data class KiteFnoContractResponse(
    val underlying: IndexUnderlying,
    val expiry: LocalDate,
    val strike: BigDecimal,
    val optionType: OptionType,
    val lotSize: Int,
    val tradingSymbol: String,
    val instrumentToken: Long,
) {
    companion object {
        fun from(result: KiteOptionContract) = KiteFnoContractResponse(
            result.contract.underlying,
            result.contract.expiry,
            result.contract.strike,
            result.contract.optionType,
            result.contract.lotSize,
            result.contract.tradingSymbol,
            result.instrumentToken,
        )
    }
}
