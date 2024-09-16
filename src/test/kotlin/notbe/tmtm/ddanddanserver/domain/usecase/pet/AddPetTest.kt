package notbe.tmtm.ddanddanserver.domain.usecase.pet

import io.mockk.every
import io.mockk.mockk
import notbe.tmtm.ddanddanserver.domain.gateway.PetGateway
import notbe.tmtm.ddanddanserver.domain.model.pet.Pet
import notbe.tmtm.ddanddanserver.domain.model.pet.PetType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class AddPetTest {
    private val petGateway: PetGateway = mockk<PetGateway>()

    private val addPet: AddPet = AddPet(petGateway)

    @Test
    fun `요청하는 펫 종류의 펫을 추가합니다`() {
        // given
        val userId: String = "ABCDEF1234567"
        val requestPetType = PetType.DOG
        val pet =
            Pet.register(
                ownerUserId = userId,
                type = requestPetType,
            )
        every { petGateway.save(any()) } returns pet

        // when
        val output = addPet.execute(AddPet.Input(userId, requestPetType))

        // then
        Assertions.assertEquals(output.pet.ownerUserId, userId)
        Assertions.assertEquals(output.pet.type, requestPetType)
    }

    @Test
    fun `펫은 exp가 0인 상태로 추가됩니다`() {
        // given
        val userId: String = "ABCDEF1234567"
        val requestPetType = PetType.CAT
        val pet =
            Pet.register(
                ownerUserId = userId,
                type = requestPetType,
            )
        every { petGateway.save(any()) } returns pet

        // when
        val output = addPet.execute(AddPet.Input(userId, requestPetType))

        // then
        Assertions.assertEquals(output.pet.getExpPercent(), 0.0)
        Assertions.assertEquals(output.pet.getLevel().level, 1)
    }
}
