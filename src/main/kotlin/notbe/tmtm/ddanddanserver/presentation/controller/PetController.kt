package notbe.tmtm.ddanddanserver.presentation.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import notbe.tmtm.ddanddanserver.domain.usecase.pet.AddPet
import notbe.tmtm.ddanddanserver.domain.usecase.pet.GetPets
import notbe.tmtm.ddanddanserver.presentation.dto.request.AddPetRequest
import notbe.tmtm.ddanddanserver.presentation.dto.response.PetResponse
import notbe.tmtm.ddanddanserver.presentation.dto.response.PetsResponse
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/v1/pets")
@Tag(name = "펫")
class PetController(
    private val getPets: GetPets,
    private val addPet: AddPet,
) {
    @PostMapping("/me")
    @Operation(summary = "펫 추가", description = "펫을 추가합니다.")
    fun addMyPet(
        authentication: Authentication,
        @RequestBody request: AddPetRequest,
    ): PetResponse {
        val result =
            addPet.execute(
                AddPet.Input(
                    ownerUserId = authentication.name,
                    petType = request.petType,
                ),
            )
        return PetResponse.fromDomain(result.pet)
    }

    @GetMapping("/me")
    @Operation(summary = "내가 소유한 펫 조회", description = "내가 소유한 펫을 모두 조회합니다.")
    fun getMyPets(authentication: Authentication): PetsResponse {
        val result =
            getPets.execute(
                GetPets.Input(
                    ownerUserId = authentication.name,
                ),
            )
        return PetsResponse.fromDomain(
            ownerUserId = authentication.name,
            pets = result.pets,
        )
    }
}
