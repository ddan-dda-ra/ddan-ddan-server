package notbe.tmtm.ddanddanserver.presentation.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import notbe.tmtm.ddanddanserver.application.service.PetService
import notbe.tmtm.ddanddanserver.presentation.dto.request.AddPetRequest
import notbe.tmtm.ddanddanserver.presentation.dto.response.PetResponse
import notbe.tmtm.ddanddanserver.presentation.dto.response.PetsResponse
import notbe.tmtm.ddanddanserver.presentation.dto.response.UserPetResponse
import org.bson.types.ObjectId
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/pets")
@Tag(name = "펫")
class PetController(
    private val petService: PetService,
) {
    @PostMapping("/me")
    @Operation(summary = "펫 추가", description = "펫을 추가합니다.")
    fun addMyPet(
        authentication: Authentication,
        @RequestBody request: AddPetRequest,
    ): PetResponse {
        val result =
            petService.addPet(
                ownerUserId = ObjectId(authentication.name),
                petType = request.petType,
            )
        return PetResponse.fromDomain(result)
    }

    @PostMapping("/me/random")
    @Operation(summary = "랜덤 펫 추가", description = "랜덤으로 펫을 추가합니다.")
    fun addRandomPet(authentication: Authentication): PetResponse {
        val result =
            petService.addRandomPet(
                ownerUserId = ObjectId(authentication.name),
            )
        return PetResponse.fromDomain(result)
    }

    @PostMapping("/{petId}/food")
    @Operation(summary = "펫 먹이 지급", description = "펫에게 먹이를 지급해 성장시킵니다.")
    fun feedPet(
        authentication: Authentication,
        @PathVariable petId: String,
    ): UserPetResponse {
        val result =
            petService.feedPet(
                ownerUserId = ObjectId(authentication.name),
                petId = ObjectId(petId),
            )
        return UserPetResponse.fromDomain(result.user, result.pet)
    }

    @PostMapping("/{petId}/play")
    @Operation(summary = "펫 놀아주기", description = "장난감을 사용해 펫과 놀아주어 성장시킵니다.")
    fun playPet(
        authentication: Authentication,
        @PathVariable petId: String,
    ): UserPetResponse {
        val result =
            petService.playPet(
                ownerUserId = ObjectId(authentication.name),
                petId = ObjectId(petId),
            )
        return UserPetResponse.fromDomain(result.user, result.pet)
    }

    @GetMapping("/me")
    @Operation(summary = "내가 소유한 펫 조회", description = "내가 소유한 펫을 모두 조회합니다.")
    fun getMyPets(authentication: Authentication): PetsResponse {
        val result =
            petService.getPets(
                ownerUserId = ObjectId(authentication.name),
            )
        return PetsResponse.fromDomain(
            ownerUserId = authentication.name,
            pets = result,
        )
    }

    @GetMapping("/{petId}")
    @Operation(summary = "펫 조회", description = "특정 펫을 조회합니다.")
    fun getPet(
        authentication: Authentication,
        @PathVariable petId: String,
    ): PetResponse {
        val result =
            petService.getPet(
                userId = ObjectId(authentication.name),
                petId = ObjectId(petId),
            )
        return PetResponse.fromDomain(result)
    }
}
