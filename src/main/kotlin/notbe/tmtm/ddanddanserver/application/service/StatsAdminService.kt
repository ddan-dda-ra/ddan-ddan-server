package notbe.tmtm.ddanddanserver.application.service

import notbe.tmtm.ddanddanserver.infrastructure.database.repository.StatsAdminRepository
import notbe.tmtm.ddanddanserver.presentation.dto.admin.DailyCountResponse
import notbe.tmtm.ddanddanserver.presentation.dto.admin.PetCountResponse
import notbe.tmtm.ddanddanserver.presentation.dto.admin.StatsAdminResponse
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZoneId

@Service
class StatsAdminService(
    private val statsAdminRepository: StatsAdminRepository,
) {
    fun getDashboard(seriesDays: Int): StatsAdminResponse {
        val today = LocalDate.now(ZONE)
        val weekAgo = today.minusDays(WEEK_OFFSET)
        val monthAgo = today.minusDays(MONTH_OFFSET)
        val seriesStart = today.minusDays((seriesDays - 1).toLong())

        return StatsAdminResponse(
            totalUsers = statsAdminRepository.countAllUsers(),
            newUsersToday = statsAdminRepository.countNewUsersBetween(today, today),
            newUsersThisWeek = statsAdminRepository.countNewUsersBetween(weekAgo, today),
            newUsersThisMonth = statsAdminRepository.countNewUsersBetween(monthAgo, today),
            activeUsersToday = statsAdminRepository.countDistinctActiveUsersBetween(today, today),
            activeUsersThisWeek = statsAdminRepository.countDistinctActiveUsersBetween(weekAgo, today),
            totalPets = statsAdminRepository.countAllPets(),
            petDistribution =
                statsAdminRepository.groupPetsByType().map {
                    PetCountResponse(type = it.type, count = it.count)
                },
            signupSeries =
                statsAdminRepository.getSignupSeries(seriesStart, today).map {
                    DailyCountResponse(date = it.date, count = it.count)
                },
        )
    }

    private companion object {
        val ZONE: ZoneId = ZoneId.of("Asia/Seoul")
        const val WEEK_OFFSET = 6L // 오늘 포함 7일
        const val MONTH_OFFSET = 29L // 오늘 포함 30일
    }
}
