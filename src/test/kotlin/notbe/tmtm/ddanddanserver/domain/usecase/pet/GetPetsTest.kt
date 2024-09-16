package notbe.tmtm.ddanddanserver.domain.usecase.pet

import io.mockk.every
import io.mockk.mockk
import notbe.tmtm.ddanddanserver.domain.gateway.PetGateway
import notbe.tmtm.ddanddanserver.domain.model.pet.Pet
import notbe.tmtm.ddanddanserver.domain.model.pet.PetType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class GetPetsTest {
    private val petGateway: PetGateway = mockk<PetGateway>()

    private val getPets: GetPets = GetPets(petGateway)

    @Test
    fun `펫 리스트 조회에 성공한다`() {
        // given
        val ownerUserId: String = "ABCDEF1234567"
        val pets: List<Pet> =
            listOf(
                Pet.register(
                    type = PetType.CAT,
                    ownerUserId = ownerUserId,
                ),
                Pet.register(
                    type = PetType.DOG,
                    ownerUserId = ownerUserId,
                ),
            )
        every { petGateway.getPetsByOwnerUserId(ownerUserId) } returns pets

        // when
        val result =
            getPets.execute(
                GetPets.Input(
                    ownerUserId = ownerUserId,
                ),
            )

        // then
        Assertions.assertEquals(2, result.pets.size)
        Assertions.assertAll(
            { Assertions.assertEquals(ownerUserId, result.pets[0].ownerUserId) },
            { Assertions.assertEquals(ownerUserId, result.pets[1].ownerUserId) },
        )
    }

    @Test
    fun `펫이 존재하지 않으면, 빈 리스트를 반환한다`() {
        // given
        val ownerUserId: String = "ABCDEF1234567"
        every { petGateway.getPetsByOwnerUserId(ownerUserId) } returns emptyList()

        // when
        val result =
            getPets.execute(
                GetPets.Input(
                    ownerUserId = ownerUserId,
                ),
            )

        // then
        Assertions.assertTrue(result.pets.isEmpty())
    }
}
