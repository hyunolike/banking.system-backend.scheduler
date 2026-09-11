package com.banking_system.scheduler_server

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    properties = [
        "banking.api.base-url=http://localhost:8182",
        "banking.api.internal-api-key=test-internal-api-key",
        // 컨텍스트 로딩 테스트에서 실제 API 서버를 호출하지 않도록 스케줄을 끈다.
        "banking.scheduler.enabled=false",
    ],
)
class SchedulerServerApplicationTests {

    @Test
    fun contextLoads() {
    }

}
