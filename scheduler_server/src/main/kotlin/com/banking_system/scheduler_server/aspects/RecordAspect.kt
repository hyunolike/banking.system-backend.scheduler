package com.banking_system.scheduler_server.aspects

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * [RecordAutoGetAPI] 가 붙은 메서드의 소요 시간을 남긴다.
 *
 * 이전 구현에는 두 가지 문제가 있었다.
 *  1. `@Around` 인데 `proceed()` 를 호출하지 않아 대상 메서드가 **아예 실행되지 않았다.**
 *  2. `start` 직후에 경과 시간을 계산해 항상 0ms 가 찍혔다.
 */
@Aspect
@Component
class RecordAspect {

    private val log = LoggerFactory.getLogger(RecordAspect::class.java)

    @Around("@annotation(com.banking_system.scheduler_server.aspects.RecordAutoGetAPI)")
    fun record(joinPoint: ProceedingJoinPoint): Any? {
        val signature = joinPoint.signature.toShortString()
        val start = System.nanoTime()

        return try {
            joinPoint.proceed().also {
                log.info("auto get api :: {} :: returned :: {} ms", signature, elapsedMillis(start))
            }
        } catch (e: Throwable) {
            // 실패도 계측 대상이다. 예외는 삼키지 않고 그대로 올려보낸다.
            log.warn("auto get api :: {} :: threw :: {} ms :: {}", signature, elapsedMillis(start), e.toString())
            throw e
        }
    }

    private fun elapsedMillis(startNanos: Long): Long =
        (System.nanoTime() - startNanos) / 1_000_000
}
