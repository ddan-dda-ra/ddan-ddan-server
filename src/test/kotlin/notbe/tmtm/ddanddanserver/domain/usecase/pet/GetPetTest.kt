package notbe.tmtm.ddanddanserver.domain.usecase.pet

import io.mockk.every
import io.mockk.mockk
import notbe.tmtm.ddanddanserver.domain.exception.PetOwnerMismatchException
import notbe.tmtm.ddanddanserver.domain.gateway.PetGateway
import notbe.tmtm.ddanddanserver.domain.model.pet.Pet
import notbe.tmtm.ddanddanserver.domain.model.pet.PetType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class GetPetTest {
    private val petGateway: PetGateway = mockk<PetGateway>()

    private val getPet: GetPet = GetPet(petGateway)

    @Test
    fun `특정 펫 조회에 성공한다`() {
        // given
        val ownerUserId = "OWNER_USER_ID"
        val petId = "PET_ID"
        val pet =
            Pet.register(
                type = PetType.CAT,
                ownerUserId = ownerUserId,
            )
        every { petGateway.getById(petId) } returns pet

        // when
        val result =
            getPet.execute(
                GetPet.Input(
                    userId = ownerUserId,
                    petId = petId,
                ),
            )

        // then
        Assertions.assertNotNull(result.pet)
        Assertions.assertEquals(ownerUserId, result.pet.ownerUserId)
    }

    @Test
    fun `소유자가 아닌 유저가 조회를 시도하면 예외를 던진다`() {
        // given
        val ownerUserId = "OWNER_USER_ID"
        val petId = "PET_ID"
        val pet =
            Pet.register(
                ownerUserId = ownerUserId,
                type = PetType.CAT,
            )
        every { petGateway.getById(petId) } returns pet

        // when & then
        Assertions.assertThrows(PetOwnerMismatchException::class.java) {
            getPet.execute(
                GetPet.Input(
                    userId = "NOT_OWNER_USER_ID",
                    petId = petId,
                ),
            )
        }
    }
}
