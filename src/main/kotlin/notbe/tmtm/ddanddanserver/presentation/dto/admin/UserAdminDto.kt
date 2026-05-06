package notbe.tmtm.ddanddanserver.presentation.dto.admin

import notbe.tmtm.ddanddanserver.domain.model.pet.PetType
import notbe.tmtm.ddanddanserver.domain.model.user.User
import org.bson.types.ObjectId
import org.springframework.data.domain.Page

data class UserAdminSummaryResponse(
    val id: String,
    val name: String?,
    val mainPetId: String?,
    val mainPetType: PetType?,
    val tickets: Int,
    val purposeCalorie: Int,
    val lastLoginAt: String,
) {
    companion object {
        fun from(
            user: User,
            mainPetType: PetType? = null,
        ): UserAdminSummaryResponse =
            UserAdminSummaryResponse(
                id = user.id.toHexString(),
                name = user.name,
                mainPetId = user.mainPetId?.toHexString(),
                mainPetType = mainPetType,
                tickets = user.tickets,
                purposeCalorie = user.purposeCalorie,
                lastLoginAt = user.lastLoginAt.toString(),
            )
    }
}

data class UserAdminDetailResponse(
    val id: String,
    val name: String?,
    val mainPetId: String?,
    val mainPetType: PetType?,
    val purposeCalorie: Int,
    val foodQuantity: Int,
    val toyQuantity: Int,
    val purposeStrict: Int,
    val tickets: Int,
    val isAppPushOn: Boolean,
    val lastLoginAt: String,
) {
    companion object {
        fun from(
            user: User,
            mainPetType: PetType? = null,
        ): UserAdminDetailResponse =
            UserAdminDetailResponse(
                id = user.id.toHexString(),
                name = user.name,
                mainPetId = user.mainPetId?.toHexString(),
                mainPetType = mainPetType,
                purposeCalorie = user.purposeCalorie,
                foodQuantity = user.foodQuantity,
                toyQuantity = user.toyQuantity,
                purposeStrict = user.purposeStrict,
                tickets = user.tickets,
                isAppPushOn = user.setting.isAppPushOn,
                lastLoginAt = user.lastLoginAt.toString(),
            )
    }
}

data class UserAdminListResponse(
    val users: List<UserAdminSummaryResponse>,
    val page: PageMetaResponse,
) {
    companion object {
        fun from(
            page: Page<User>,
            mainPetTypes: Map<ObjectId, PetType> = emptyMap(),
        ): UserAdminListResponse =
            UserAdminListResponse(
                users =
                    page.content.map { user ->
                        UserAdminSummaryResponse.from(
                            user = user,
                            mainPetType = user.mainPetId?.let { mainPetTypes[it] },
                        )
                    },
                page = PageMetaResponse.from(page),
            )
    }
}

data class PageMetaResponse(
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
) {
    companion object {
        fun from(page: Page<*>): PageMetaResponse =
            PageMetaResponse(
                page = page.number,
                size = page.size,
                totalElements = page.totalElements,
                totalPages = page.totalPages,
            )
    }
}
