package notbe.tmtm.ddanddanserver.domain.gateway

import notbe.tmtm.ddanddanserver.domain.model.ranking.PeriodType
import notbe.tmtm.ddanddanserver.domain.model.ranking.RankingCriteria
import notbe.tmtm.ddanddanserver.domain.model.ranking.UserStat

interface UserStatGateway {
    fun getRanking(
        criteria: RankingCriteria,
        periodType: PeriodType,
    ): List<UserStat>
}
