package com.banking_system.scheduler_server.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
@EnableConfigurationProperties(BankingApiProperties::class)
class RestTemplateConfig {

    /**
     * 타임아웃 없는 RestTemplate 을 매 실행마다 새로 만들면
     * API 서버가 응답하지 않을 때 스케줄러 스레드가 그대로 묶인다.
     * 빈으로 한 번만 만들고 타임아웃을 반드시 건다.
     */
    @Bean
    fun bankingApiRestTemplate(
        builder: RestTemplateBuilder,
        properties: BankingApiProperties,
    ): RestTemplate =
        builder
            .rootUri(properties.baseUrl.trimEnd('/'))
            .setConnectTimeout(properties.connectTimeout)
            .setReadTimeout(properties.readTimeout)
            .build()
}
