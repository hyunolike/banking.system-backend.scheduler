package com.banking_system.scheduler_server.aspects

/**
 * 붙은 메서드의 실행 시간을 기록한다. ([RecordAspect] 참고)
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RecordAutoGetAPI
