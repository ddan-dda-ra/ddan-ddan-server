package notbe.tmtm.ddanddanserver.presentation.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import notbe.tmtm.ddanddanserver.domain.usecase.pet.AddPet
import notbe.tmtm.ddanddanserver.domain.usecase.pet.AddRandomPet
import notbe.tmtm.ddanddanserver.domain.usecase.pet.GetPets
import notbe.tmtm.ddanddanserver.presentation.dto.request.AddPetRequest
import notbe.tmtm.ddanddanserver.presentation.dto.response.PetResponse
import notbe.tmtm.ddanddanserver.presentation.dto.response.PetsResponse
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/pets")
@Tag(name = "펫")
class PetController(
    private val addPet: AddPet,
    private val addRandomPet: AddRandomPet,
    private val getPets: GetPets,
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

    @PostMapping("/me/random")
    @Operation(summary = "랜덤 펫 추가", description = "랜덤으로 펫을 추가합니다.")
    fun addRandomPet(authentication: Authentication): PetResponse {
        val result =
            addRandomPet.execute(
                AddRandomPet.Input(
                    ownerUserId = authentication.name,
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
