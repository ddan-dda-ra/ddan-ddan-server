package notbe.tmtm.ddanddanserver.config

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import org.springframework.core.annotation.Order

// SecurityFilterChain Order 매트릭스 회귀 방지 테스트.
//
// Spring Security는 등록된 SecurityFilterChain을 @Order 순서로 평가하고 첫 매치 chain만 적용한다.
// 따라서 광범위한 securityMatcher (예: /v1/all)는 좁은 매처 (예: /v1/auth/all)보다 Order가 커야 한다.
//
// PR #302 사고: 어드민 작업 중 loginFilterChain Order가 0→2로 밀려 apiFilterChain(Order=1)이
// /v1/auth/login에 먼저 매치되어 401로 차단됨. 단위 테스트로는 wiring 회귀를 못 잡으므로
// reflection 기반으로 Order 매트릭스만이라도 강제한다.
class WebSecurityConfigOrderTest : FunSpec({
    val orders =
        WebSecurityConfig::class
            .java
            .declaredMethods
            .filter { it.returnType.simpleName == "SecurityFilterChain" }
            .associate { method ->
                method.name to (method.getAnnotation(Order::class.java)?.value ?: error("${method.name} has no @Order"))
            }

    test("loginFilterChain은 apiFilterChain보다 Order가 작아야 한다 (좁은 /v1/auth 매처가 광범위 /v1 매처보다 먼저 매치되도록)") {
        orders["loginFilterChain"]!! shouldBeLessThan orders["apiFilterChain"]!!
    }

    test("등록된 SecurityFilterChain이 정확히 2개여야 한다") {
        orders.keys shouldBe setOf("loginFilterChain", "apiFilterChain")
    }
})
