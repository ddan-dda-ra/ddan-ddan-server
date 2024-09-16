package notbe.tmtm.ddanddanserver.presentation.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import notbe.tmtm.ddanddanserver.domain.model.User
import notbe.tmtm.ddanddanserver.domain.model.pet.Pet

@Schema(description = "유저 & 펫 응답 DTO")
data class UserPetResponse(
    @Schema(description = "유저")
    val user: UserResponse,
    @Schema(description = "펫")
    val pet: PetResponse,
) {
    companion object {
        fun fromDomain(
            user: User,
            pet: Pet,
        ) = UserPetResponse(
            user = UserResponse.fromDomain(user),
            pet = PetResponse.fromDomain(pet),
        )
    }
}
