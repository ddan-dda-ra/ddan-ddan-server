package notbe.tmtm.ddanddanserver.domain.usecase.pet

import io.mockk.every
import io.mockk.mockk
import notbe.tmtm.ddanddanserver.domain.exception.PetMaxLevelException
import notbe.tmtm.ddanddanserver.domain.exception.PetOwnerMismatchException
import notbe.tmtm.ddanddanserver.domain.exception.UserToyQuantityLackException
import notbe.tmtm.ddanddanserver.domain.gateway.PetGateway
import notbe.tmtm.ddanddanserver.domain.gateway.UserGateway
import notbe.tmtm.ddanddanserver.domain.model.User
import notbe.tmtm.ddanddanserver.domain.model.pet.Pet
import notbe.tmtm.ddanddanserver.domain.model.pet.PetType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PlayPetTest {
    private val userGateway: UserGateway = mockk<UserGateway>()
    private val petGateway: PetGateway = mockk<PetGateway>()

    private val playPet = PlayPet(userGateway, petGateway)

    @Test
    fun `펫에게 장난감을 사용해 성장시킨다`() {
        // given
        val user = createUser(toyQuantity = 10)
        val pet = createPet(exp = 0)
        every { userGateway.getById(user.id) } returns user
        every { petGateway.getById(pet.id) } returns pet
        every { userGateway.save(user) } returns user
        every { petGateway.save(pet) } returns pet

        // when
        val result = playPet.execute(PlayPet.Input("USER_ID", "PET_ID"))

        // then
        assertEquals(500, result.pet.exp)
    }

    @Test
    fun `펫의 소유자가 다르면 예외를 던진다`() {
        // given
        val user = createUser(id = "USER_ID")
        val pet = createPet(ownerUserId = "ANOTHER_USER_ID")
        every { userGateway.getById(user.id) } returns user
        every { petGateway.getById(pet.id) } returns pet

        // when & then
        Assertions.assertThrows(PetOwnerMismatchException::class.java) {
            playPet.execute(PlayPet.Input(user.id, pet.id))
        }
    }

    @Test
    fun `펫이 최대 레벨이면 예외를 던진다`() {
        // given
        val user = createUser()
        val pet = createPet(exp = Pet.Level.MAX_EXP)
        every { userGateway.getById(user.id) } returns user
        every { petGateway.getById(pet.id) } returns pet

        // when & then
        Assertions.assertThrows(PetMaxLevelException::class.java) {
            playPet.execute(PlayPet.Input(user.id, pet.id))
        }
    }

    @Test
    fun `장난감이 부족하면 예외를 던진다`() {
        // given
        val user = createUser(toyQuantity = 0)
        val pet = createPet()
        every { userGateway.getById(user.id) } returns user
        every { petGateway.getById(pet.id) } returns pet

        // when & then
        Assertions.assertThrows(UserToyQuantityLackException::class.java) {
            playPet.execute(PlayPet.Input(user.id, pet.id))
        }
    }

    private fun createUser(
        id: String = "USER_ID",
        name: String = "홍길동",
        toyQuantity: Int = 10,
    ): User =
        User(
            id = id,
            name = name,
            toyQuantity = toyQuantity,
        )

    private fun createPet(
        id: String = "PET_ID",
        type: PetType = PetType.DOG,
        ownerUserId: String = "USER_ID",
        exp: Int = 0,
    ): Pet =
        Pet(
            id = id,
            type = type,
            ownerUserId = ownerUserId,
            exp = exp,
        )
}
