package com.shivam.tradingbot.application.port.out

import com.shivam.tradingbot.domain.fno.IndexUnderlying
import com.shivam.tradingbot.domain.fno.OptionContract
import com.shivam.tradingbot.domain.fno.OptionType
import java.math.BigDecimal
import java.time.LocalDate

interface KiteOptionContractLookupPort {
    fun find(query: OptionContractQuery): KiteOptionContract
    fun availableExpiries(underlying: IndexUnderlying): List<LocalDate>
    fun availableStrikes(underlying: IndexUnderlying, expiry: LocalDate, optionType: OptionType): List<BigDecimal>
}

data class OptionContractQuery(
    val underlying: IndexUnderlying,
    val expiry: LocalDate,
    val strike: BigDecimal,
    val optionType: OptionType,
)

data class KiteOptionContract(
    val contract: OptionContract,
    val instrumentToken: Long,
)
