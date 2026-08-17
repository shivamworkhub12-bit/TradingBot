package com.shivam.tradingbot.application.usecase

import com.shivam.tradingbot.application.port.out.KiteHistoricalDataPort
import com.shivam.tradingbot.application.port.out.KiteHistoricalDataRequest
import com.shivam.tradingbot.domain.model.Candle

class FetchKiteHistoricalCandlesUseCase(
    private val kiteHistoricalData: KiteHistoricalDataPort,
) {
    fun execute(request: KiteHistoricalDataRequest): List<Candle> {
        require(request.instrumentToken > 0) { "instrumentToken must be positive" }
        require(request.from <= request.to) { "from must not be after to" }
        return kiteHistoricalData.load(request)
    }
}
