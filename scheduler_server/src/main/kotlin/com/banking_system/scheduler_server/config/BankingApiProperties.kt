package com.banking_system.scheduler_server.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * API 서버 호출 설정.
 *
 * 이전에는 http://localhost:8182 주소가 코드에 박혀 있어
 * 두 서버가 같은 호스트에 있을 때만 동작했다.
 *
 * @param baseUrl        API 서버 기본 URL (예: http://10.0.0.5:8182)
 * @param internalApiKey 내부 API(api/internal) 호출용 공유 키
 * @param connectTimeout 커넥션 수립 타임아웃
 * @param readTimeout    응답 대기 타임아웃
 */
@ConfigurationProperties(prefix = "banking.api")
data class BankingApiProperties(
    val baseUrl: String,
    val internalApiKey: String,
    val connectTimeout: Duration = Duration.ofSeconds(2),
    val readTimeout: Duration = Duration.ofSeconds(5),
)
