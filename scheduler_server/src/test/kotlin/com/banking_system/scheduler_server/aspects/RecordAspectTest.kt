package com.banking_system.scheduler_server.aspects

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory

/**
 * 회귀 테스트: `@Around` 어드바이스가 `proceed()` 를 호출하지 않아
 * 스케줄 작업 본문이 통째로 실행되지 않던 버그를 잡아둔다.
 */
class RecordAspectTest {

    open class Target {
        var invoked = 0

        @RecordAutoGetAPI
        open fun work(): String {
            invoked++
            return "done"
        }

        @RecordAutoGetAPI
        open fun boom(): String {
            invoked++
            throw IllegalStateException("boom")
        }

        open fun notAnnotated(): String {
            invoked++
            return "plain"
        }
    }

    private fun proxy(target: Target): Target {
        val factory = AspectJProxyFactory(target)
        factory.addAspect(RecordAspect())
        return factory.getProxy()
    }

    @Test
    @DisplayName("어드바이스가 대상 메서드를 실제로 실행하고 반환값을 그대로 돌려준다")
    fun proceedsAndReturnsValue() {
        val target = Target()

        val result = proxy(target).work()

        assertThat(target.invoked).isEqualTo(1)
        assertThat(result).isEqualTo("done")
    }

    @Test
    @DisplayName("대상 메서드가 던진 예외를 삼키지 않는다")
    fun rethrowsException() {
        val target = Target()

        assertThatThrownBy { proxy(target).boom() }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("boom")

        assertThat(target.invoked).isEqualTo(1)
    }

    @Test
    @DisplayName("애노테이션이 없는 메서드도 정상 동작한다")
    fun passesThroughUnannotatedMethod() {
        val target = Target()

        assertThat(proxy(target).notAnnotated()).isEqualTo("plain")
        assertThat(target.invoked).isEqualTo(1)
    }
}
