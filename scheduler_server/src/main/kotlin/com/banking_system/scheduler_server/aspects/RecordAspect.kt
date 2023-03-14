package com.banking_system.scheduler_server.aspects

import org.apache.juli.logging.LogFactory
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.stereotype.Component

@Aspect
@Component
class RecordAspect {
    var logger: org.apache.juli.logging.Log = LogFactory.getLog(RecordAspect::class.java)

    @Around("@annotation(RecordAutoGetAPI)")
    fun getAPI(joinPoint: ProceedingJoinPoint) {
        val start = System.currentTimeMillis()
        val signature = joinPoint.signature.toShortString()

        val duration = System.currentTimeMillis() - start
        logger.info("auto get api :: $signature :: duration :: $duration ms")
    }
}