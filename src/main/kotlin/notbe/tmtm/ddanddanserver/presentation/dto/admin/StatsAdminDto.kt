package notbe.tmtm.ddanddanserver.presentation.dto.admin

data class StatsAdminResponse(
    val totalUsers: Long,
    val newUsersToday: Long,
    val newUsersThisWeek: Long,
    val newUsersThisMonth: Long,
    val activeUsersToday: Long,
    val activeUsersThisWeek: Long,
    val totalPets: Long,
    val petDistribution: List<PetCountResponse>,
    val signupSeries: List<DailyCountResponse>,
)

data class PetCountResponse(
    val type: String,
    val count: Long,
)

data class DailyCountResponse(
    val date: String,
    val count: Long,
)
