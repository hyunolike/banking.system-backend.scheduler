package com.banking_system.scheduler_server

import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@EnableAsync
class ScheduledTasks {

    // 작업 예약
    @Scheduled(cron = "0/1 * * * * *")
    fun taskScheduler() = println("1초마다 실행")
}