package com.banking_system.scheduler_server

import com.banking_system.scheduler_server.aspects.RecordAutoGetAPI
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForObject
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
@EnableAsync
class ScheduledTasks {

    // 작업 예약
    @RecordAutoGetAPI
    @Scheduled(cron = "0/10 * * * * *")
    fun taskScheduler() {
         var uri: URI = UriComponentsBuilder
             .fromUriString("http://localhost:8182/api/userDatas")
             .encode()
             .build()
             .toUri()

        var restTemplate: RestTemplate = RestTemplate()

        return restTemplate.getForObject(uri)
    }
}