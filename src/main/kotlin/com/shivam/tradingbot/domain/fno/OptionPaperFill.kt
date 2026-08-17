package com.shivam.tradingbot.domain.fno

import java.time.Instant
import java.util.UUID

data class OptionPaperFill(val id: UUID, val order: OptionOrderIntent, val filledAt: Instant)
