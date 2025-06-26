package notbe.tmtm.ddanddanserver.application.service

import notbe.tmtm.ddanddanserver.domain.model.ranking.PeriodType
import notbe.tmtm.ddanddanserver.domain.model.ranking.RankingCriteria
import notbe.tmtm.ddanddanserver.domain.model.ranking.RankingResult
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserStatRepository
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserRankingService(
    private val userStatRepository: UserStatRepository,
) {
    @Transactional(readOnly = true)
    fun getRanking(
        userId: ObjectId,
        criteria: RankingCriteria,
        periodType: PeriodType,
    ): RankingResult {
        val userStats = userStatRepository.getAllRankingBy(criteria, periodType).map { it.toDomain() }
        return RankingResult.create(userStats, criteria, userId)
    }
}
