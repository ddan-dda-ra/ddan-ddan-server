package notbe.tmtm.ddanddanserver.domain.usecase.user

import io.mockk.every
import io.mockk.mockk
import jakarta.persistence.EntityNotFoundException
import notbe.tmtm.ddanddanserver.domain.gateway.UserGateway
import notbe.tmtm.ddanddanserver.domain.model.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import kotlin.test.Test

class UpdateUserTest {
    private val userGateway: UserGateway = mockk<UserGateway>()

    private val updateUser: UpdateUser = UpdateUser(userGateway)

    @Test
    fun `유저 정보 수정에 성공한다`() {
        // given
        val user =
            User(
                id = "ABCDEF1234567",
                name = "oldUserName",
                purposeCalorie = 100,
            )

        every { userGateway.getById(user.id) } returns user
        every { userGateway.update(user) } returns user

        // when
        val result =
            updateUser.execute(
                UpdateUser.UpdateUserInput(
                    userId = "ABCDEF1234567",
                    name = "newUserName",
                    purposeCalorie = 200,
                ),
            )

        // then
        assertEquals("newUserName", result.user.name)
        assertEquals(200, result.user.purposeCalorie)
    }

    @Test
    fun `userId가 존재하지 않는 경우 EntityNotFoundException가 던져진다`() {
        // given
        val user =
            User(
                id = "ABCDEF1234567",
                name = "oldUserName",
                purposeCalorie = 100,
            )

        every { userGateway.getById(user.id) } throws EntityNotFoundException()

        // when & then
        assertThrows(EntityNotFoundException::class.java) {
            updateUser.execute(
                UpdateUser.UpdateUserInput(
                    userId = "ABCDEF1234567",
                    name = "newUserName",
                    purposeCalorie = 200,
                ),
            )
        }
    }
}
