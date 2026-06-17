package dev.feedi

import java.time.Instant

public data class FeediReceipt(
    public val id: String,
    public val receivedAt: Instant,
)
