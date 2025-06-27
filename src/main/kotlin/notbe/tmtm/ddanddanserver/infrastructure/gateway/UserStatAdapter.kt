package notbe.tmtm.ddanddanserver.infrastructure.gateway

import notbe.tmtm.ddanddanserver.domain.model.ranking.PeriodType
import notbe.tmtm.ddanddanserver.domain.model.ranking.RankingCriteria
import notbe.tmtm.ddanddanserver.domain.model.ranking.UserStat
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserStatRepository
import org.springframework.stereotype.Component

@Component
class UserStatAdapter(
    private val userStatRepository: UserStatRepository,
) {
    fun getRanking(
        criteria: RankingCriteria,
        periodType: PeriodType,
    ): List<UserStat> = userStatRepository.getAllRankingBy(criteria, periodType).map { it.toDomain() }
}
