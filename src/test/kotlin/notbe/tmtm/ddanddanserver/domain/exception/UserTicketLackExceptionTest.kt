package notbe.tmtm.ddanddanserver.domain.exception

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class UserTicketLackExceptionTest : FunSpec({

    test("UserTicketLackException은 UserException을 상속해야 한다") {
        val exception = UserTicketLackException()

        exception.shouldBeInstanceOf<UserException>()
        exception.shouldBeInstanceOf<CustomException>()
    }

    test("UserTicketLackException은 올바른 에러 코드를 가져야 한다") {
        val exception = UserTicketLackException()

        exception.errorCode shouldBe ErrorCode.USER_TICKET_LACK
        exception.message shouldBe ErrorCode.USER_TICKET_LACK.message
    }

    test("UserTicketLackException은 데이터를 포함할 수 있어야 한다") {
        val testData = mapOf("userId" to "123", "currentTickets" to 0, "requiredTickets" to 1)
        val exception = UserTicketLackException(testData)

        exception.errorCode shouldBe ErrorCode.USER_TICKET_LACK
        exception.data shouldBe testData
        exception.message shouldBe ErrorCode.USER_TICKET_LACK.message
    }
})
