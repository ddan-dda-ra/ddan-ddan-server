package notbe.tmtm.ddanddanserver.infrastructure.database.repository

import notbe.tmtm.ddanddanserver.domain.model.ranking.PeriodType
import notbe.tmtm.ddanddanserver.domain.model.ranking.RankingCriteria
import notbe.tmtm.ddanddanserver.infrastructure.database.entity.UserStatEntity

interface UserStatRepository {
    fun getRanking(
        criteria: RankingCriteria,
        periodType: PeriodType,
    ): List<UserStatEntity>
}
