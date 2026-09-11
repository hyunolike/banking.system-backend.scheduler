package com.banking_system.scheduler_server.client

import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * API 서버 `/api/internal/stats/snapshot` 응답.
 */
data class SystemSnapshot(
    val userCount: Long,
    val accountCount: Long,
    val totalBalance: BigDecimal,
    val generatedAt: LocalDateTime,
)
