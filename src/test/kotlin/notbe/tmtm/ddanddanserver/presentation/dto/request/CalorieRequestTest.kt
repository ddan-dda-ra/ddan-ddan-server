package notbe.tmtm.ddanddanserver.presentation.dto.request

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CalorieRequestTest : FunSpec({
    test("0과 10000은 허용한다") {
        CalorieRequest(0).calorie shouldBe 0
        CalorieRequest(10_000).calorie shouldBe 10_000
    }

    test("범위를 벗어나면 IllegalArgumentException이 발생한다") {
        shouldThrow<IllegalArgumentException> { CalorieRequest(-1) }
        shouldThrow<IllegalArgumentException> { CalorieRequest(10_001) }
    }
})
