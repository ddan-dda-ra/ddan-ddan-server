package notbe.tmtm.ddanddanserver.domain.usecase.user

import io.mockk.every
import io.mockk.mockk
import jakarta.persistence.EntityNotFoundException
import notbe.tmtm.ddanddanserver.domain.gateway.UserGateway
import notbe.tmtm.ddanddanserver.domain.model.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import kotlin.test.Test

class GetUserTest {
    private val userGateway: UserGateway = mockk<UserGateway>()

    private val getUser: GetUser = GetUser(userGateway)

    @Test
    fun `유저 정보 조회에 성공한다`() {
        // given
        val existUser =
            User(
                id = "ABCDEF1234567",
                name = "userName",
                purposeCalorie = 100,
            )

        every { userGateway.getById(existUser.id) } returns existUser

        // when
        val result =
            getUser.execute(
                GetUser.GetUserInput(
                    userId = "ABCDEF1234567",
                ),
            )

        // then
        assertEquals("ABCDEF1234567", result.user.id)
    }

    @Test
    fun `userId가 존재하지 않는 경우 EntityNotFoundException가 던져진다`() {
        // given
        val notExistUserId = "ABCDEF1234567"

        every { userGateway.getById(notExistUserId) } throws EntityNotFoundException()

        // when & then
        assertThrows(EntityNotFoundException::class.java) {
            getUser.execute(
                GetUser.GetUserInput(
                    userId = notExistUserId,
                ),
            )
        }
    }
}
