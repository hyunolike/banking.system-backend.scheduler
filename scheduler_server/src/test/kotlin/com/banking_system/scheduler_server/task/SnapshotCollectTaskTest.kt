package com.banking_system.scheduler_server.task

import com.banking_system.scheduler_server.client.BankingApiClient
import com.banking_system.scheduler_server.client.SystemSnapshot
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.web.client.ResourceAccessException
import java.math.BigDecimal
import java.time.LocalDateTime

class SnapshotCollectTaskTest {

    private val client = mockk<BankingApiClient>()
    private val task = SnapshotCollectTask(client)

    @Test
    @DisplayName("스냅샷을 조회한다")
    fun collects() {
        every { client.fetchSnapshot() } returns SystemSnapshot(
            userCount = 1,
            accountCount = 2,
            totalBalance = BigDecimal("100.0000"),
            generatedAt = LocalDateTime.now(),
        )

        task.collectSnapshot()

        verify(exactly = 1) { client.fetchSnapshot() }
    }

    @Test
    @DisplayName("API 호출이 실패해도 예외를 밖으로 던지지 않아 스케줄이 계속 돈다")
    fun survivesApiFailure() {
        every { client.fetchSnapshot() } throws ResourceAccessException("connect timed out")

        task.collectSnapshot()

        verify(exactly = 1) { client.fetchSnapshot() }
    }
}
