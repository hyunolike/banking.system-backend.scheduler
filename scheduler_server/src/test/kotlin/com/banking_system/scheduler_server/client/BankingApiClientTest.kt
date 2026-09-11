package com.banking_system.scheduler_server.client

import com.banking_system.scheduler_server.config.BankingApiProperties
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.time.Duration

class BankingApiClientTest {

    private val properties = BankingApiProperties(
        baseUrl = "http://api.internal.test:8182",
        internalApiKey = "test-internal-api-key",
        connectTimeout = Duration.ofSeconds(1),
        readTimeout = Duration.ofSeconds(1),
    )

    private lateinit var restTemplate: RestTemplate
    private lateinit var server: MockRestServiceServer
    private lateinit var client: BankingApiClient

    @BeforeEach
    fun setUp() {
        restTemplate = RestTemplateBuilder()
            .rootUri(properties.baseUrl)
            .setConnectTimeout(properties.connectTimeout)
            .setReadTimeout(properties.readTimeout)
            .build()
        server = MockRestServiceServer.createServer(restTemplate)
        client = BankingApiClient(restTemplate, properties)
    }

    @Test
    @DisplayName("내부 API 키 헤더를 붙여 스냅샷을 조회한다")
    fun fetchSnapshot() {
        server.expect(requestTo("http://api.internal.test:8182/api/internal/stats/snapshot"))
            .andExpect(method(org.springframework.http.HttpMethod.GET))
            .andExpect(header(BankingApiClient.INTERNAL_API_KEY_HEADER, "test-internal-api-key"))
            .andRespond(
                withSuccess(
                    """
                    {
                      "userCount": 3,
                      "accountCount": 5,
                      "totalBalance": 12345.6789,
                      "generatedAt": "2024-05-01T10:15:30"
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON,
                ),
            )

        val snapshot = client.fetchSnapshot()

        assertThat(snapshot.userCount).isEqualTo(3)
        assertThat(snapshot.accountCount).isEqualTo(5)
        assertThat(snapshot.totalBalance).isEqualByComparingTo("12345.6789")
        server.verify()
    }

    @Test
    @DisplayName("키가 틀려 401 이 오면 예외로 전달된다")
    fun unauthorized() {
        server.expect(requestTo("http://api.internal.test:8182/api/internal/stats/snapshot"))
            .andRespond(withStatus(HttpStatus.UNAUTHORIZED))

        assertThatThrownBy { client.fetchSnapshot() }
            .isInstanceOf(HttpClientErrorException.Unauthorized::class.java)
    }
}
