package notbe.tmtm.ddanddanserver.infrastructure.database.entity

import notbe.tmtm.ddanddanserver.domain.model.pet.Pet
import notbe.tmtm.ddanddanserver.domain.model.ranking.UserStat
import notbe.tmtm.ddanddanserver.domain.model.user.User

data class UserStatEntity(
    val user: User,
    val mainPet: Pet,
    val totalCalories: Int,
    val totalSucceededDays: Int,
) {
    companion object {
        fun fromDomain(userStat: UserStat): UserStatEntity =
            UserStatEntity(
                user = userStat.user,
                mainPet = userStat.mainPet,
                totalCalories = userStat.totalCalories,
                totalSucceededDays = userStat.totalSucceededDays,
            )
    }

    fun toDomain(): UserStat =
        UserStat(
            user = user,
            mainPet = mainPet,
            totalCalories = totalCalories,
            totalSucceededDays = totalSucceededDays,
        )

}
