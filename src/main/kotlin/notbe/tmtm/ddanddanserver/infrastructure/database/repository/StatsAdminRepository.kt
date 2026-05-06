package notbe.tmtm.ddanddanserver.infrastructure.database.repository

import notbe.tmtm.ddanddanserver.domain.model.pet.PetType
import java.time.LocalDate

interface StatsAdminRepository {
    fun countAllUsers(): Long

    fun countAllPets(): Long

    /** [from, to] 두 날짜 사이(둘 다 포함, KST 기준)에 가입한 유저 수. ObjectId timestamp 기반. */
    fun countNewUsersBetween(from: LocalDate, to: LocalDate): Long

    /** [from, to] 두 날짜 사이(둘 다 포함, KST 기준)에 daily_calories에 기록을 남긴 distinct 유저 수. */
    fun countDistinctActiveUsersBetween(from: LocalDate, to: LocalDate): Long

    /** 펫 종류별 분포. count desc. */
    fun groupPetsByType(): List<StatsPetTypeCount>

    /** [from, to] 두 날짜 사이(KST)의 일별 신규 가입자 수. 누락일은 0으로 채워서 반환. */
    fun getSignupSeries(from: LocalDate, to: LocalDate): List<StatsDailyCount>
}

data class StatsPetTypeCount(
    val type: PetType,
    val count: Long,
)

data class StatsDailyCount(
    val date: String,
    val count: Long,
)
