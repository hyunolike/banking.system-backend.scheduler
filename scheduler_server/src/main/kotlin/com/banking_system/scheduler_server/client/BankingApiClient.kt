package com.banking_system.scheduler_server.client

import com.banking_system.scheduler_server.config.BankingApiProperties
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

/**
 * API 서버 내부 엔드포인트 클라이언트.
 */
@Component
class BankingApiClient(
    private val restTemplate: RestTemplate,
    private val properties: BankingApiProperties,
) {

    companion object {
        const val INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key"
        const val SNAPSHOT_PATH = "/api/internal/stats/snapshot"
    }

    fun fetchSnapshot(): SystemSnapshot {
        val headers = HttpHeaders().apply {
            set(INTERNAL_API_KEY_HEADER, properties.internalApiKey)
        }

        return restTemplate.exchange(
            SNAPSHOT_PATH,
            HttpMethod.GET,
            HttpEntity<Void>(headers),
            SystemSnapshot::class.java,
        ).body ?: error("빈 응답을 받았습니다: $SNAPSHOT_PATH")
    }
}
