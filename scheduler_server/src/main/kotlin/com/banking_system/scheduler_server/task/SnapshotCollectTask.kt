package com.banking_system.scheduler_server.task

import com.banking_system.scheduler_server.aspects.RecordAutoGetAPI
import com.banking_system.scheduler_server.client.BankingApiClient
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException

/**
 * API 서버의 현황 스냅샷을 주기적으로 수집한다.
 *
 * 예전에는 10초마다 인증 없는 `/api/userDatas` 로 **사용자 목록 전체**를 긁어왔다.
 * 지금은 내부 API 키로 집계값만 받아온다.
 */
@Component
@ConditionalOnProperty(prefix = "banking.scheduler", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class SnapshotCollectTask(
    private val bankingApiClient: BankingApiClient,
) {

    private val log = LoggerFactory.getLogger(SnapshotCollectTask::class.java)

    @RecordAutoGetAPI
    @Scheduled(cron = "\${banking.scheduler.snapshot-cron:0 * * * * *}")
    fun collectSnapshot() {
        try {
            val snapshot = bankingApiClient.fetchSnapshot()
            log.info(
                "snapshot collected :: users={} accounts={} totalBalance={} generatedAt={}",
                snapshot.userCount,
                snapshot.accountCount,
                snapshot.totalBalance,
                snapshot.generatedAt,
            )
        } catch (e: RestClientException) {
            // 한 번 실패했다고 스케줄이 멈추면 안 된다. 다음 주기에 다시 시도한다.
            log.error("snapshot collect failed :: {}", e.message)
        }
    }
}
