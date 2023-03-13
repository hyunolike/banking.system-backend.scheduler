package com.banking_system.scheduler_server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
class SchedulerServerApplication

fun main(args: Array<String>) {
    runApplication<SchedulerServerApplication>(*args)
}
