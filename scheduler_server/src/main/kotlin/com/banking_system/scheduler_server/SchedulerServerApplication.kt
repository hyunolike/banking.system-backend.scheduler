package com.banking_system.scheduler_server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SchedulerServerApplication

fun main(args: Array<String>) {
    runApplication<SchedulerServerApplication>(*args)
}
