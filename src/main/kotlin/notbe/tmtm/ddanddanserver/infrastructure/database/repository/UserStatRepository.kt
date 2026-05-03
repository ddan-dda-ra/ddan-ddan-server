package notbe.tmtm.ddanddanserver.infrastructure.database.repository

import notbe.tmtm.ddanddanserver.domain.model.ranking.PeriodType
import notbe.tmtm.ddanddanserver.domain.model.ranking.RankingCriteria
import notbe.tmtm.ddanddanserver.domain.model.ranking.UserStat
import notbe.tmtm.ddanddanserver.infrastructure.database.entity.UserStatEntity
import java.time.LocalDate

interface UserStatRepository {
    fun findAllRankingBy(
        criteria: RankingCriteria,
        periodType: PeriodType,
    ): List<UserStatEntity>

    fun findRankingByDateRange(
        criteria: RankingCriteria,
        startDate: LocalDate,
        endDate: LocalDate,
        limit: Int,
    ): List<UserStatEntity>
}

fun UserStatRepository.getAllRankingBy(
    criteria: RankingCriteria,
    periodType: PeriodType,
): List<UserStat> {
    return findAllRankingBy(criteria, periodType).map { it.toDomain() }
}
