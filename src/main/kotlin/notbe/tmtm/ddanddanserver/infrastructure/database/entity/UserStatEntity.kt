package notbe.tmtm.ddanddanserver.infrastructure.database.entity

import notbe.tmtm.ddanddanserver.domain.model.pet.PetType
import notbe.tmtm.ddanddanserver.domain.model.ranking.UserStat

data class UserStatEntity(
    val userId: String,
    val userName: String?,
    val mainPetType: String?,
    val totalCalories: Int,
    val totalSucceededDays: Int,
) {
    companion object {
        fun fromDomain(userStat: UserStat): UserStatEntity =
            UserStatEntity(
                userId = userStat.userId,
                userName = userStat.userName,
                mainPetType = userStat.mainPetType.name,
                totalCalories = userStat.totalCalories,
                totalSucceededDays = userStat.totalSucceededDays,
            )
    }

    fun toDomain(): UserStat {
        val userStat =
            UserStat(
                userId = userId,
                userName = userName ?: throw IllegalStateException("userName is null"),
                mainPetType = mainPetType?.let { PetType.valueOf(it) } ?: throw IllegalStateException("mainPetType is null"),
                totalCalories = totalCalories,
                totalSucceededDays = totalSucceededDays,
            )
        return userStat
    }
}
