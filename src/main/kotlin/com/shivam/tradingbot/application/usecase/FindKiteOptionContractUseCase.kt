package com.shivam.tradingbot.application.usecase

import com.shivam.tradingbot.application.port.out.KiteOptionContract
import com.shivam.tradingbot.application.port.out.KiteOptionContractLookupPort
import com.shivam.tradingbot.application.port.out.OptionContractQuery
import com.shivam.tradingbot.domain.fno.IndexUnderlying
import java.time.LocalDate
import java.math.BigDecimal
import com.shivam.tradingbot.domain.fno.OptionType

class FindKiteOptionContractUseCase(
    private val kiteOptionContractLookup: KiteOptionContractLookupPort,
) {
    fun execute(query: OptionContractQuery): KiteOptionContract = kiteOptionContractLookup.find(query)

    fun availableExpiries(underlying: IndexUnderlying): List<LocalDate> =
        kiteOptionContractLookup.availableExpiries(underlying)

    fun availableStrikes(underlying: IndexUnderlying, expiry: LocalDate, optionType: OptionType): List<BigDecimal> =
        kiteOptionContractLookup.availableStrikes(underlying, expiry, optionType)
}
