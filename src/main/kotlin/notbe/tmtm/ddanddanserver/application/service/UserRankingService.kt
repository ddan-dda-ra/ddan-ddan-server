package notbe.tmtm.ddanddanserver.application.service

import notbe.tmtm.ddanddanserver.domain.model.ranking.PeriodType
import notbe.tmtm.ddanddanserver.domain.model.ranking.RankingBoard
import notbe.tmtm.ddanddanserver.domain.model.ranking.RankingCriteria
import notbe.tmtm.ddanddanserver.domain.model.ranking.RankingResult
import notbe.tmtm.ddanddanserver.domain.model.ranking.UserRanking
import notbe.tmtm.ddanddanserver.domain.model.ranking.UserStat
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserStatRepository
import notbe.tmtm.ddanddanserver.infrastructure.gateway.RankingBoardAdapter
import notbe.tmtm.ddanddanserver.infrastructure.gateway.UserStatAdapter
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserRankingService(
    private val userStatRepository: UserStatRepository,
    private val rankingBoardAdapter: RankingBoardAdapter,
    private val userStatAdapter: UserStatAdapter,
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

    @Transactional(readOnly = true)
    fun getRankingBoard(periodType: PeriodType): RankingBoard {
        return rankingBoardAdapter.getById(periodType)
    }

    @Transactional(readOnly = true)
    fun getTopRanking(criteria: RankingCriteria, periodType: PeriodType): UserRanking? {
        return userStatAdapter.getRanking(criteria, periodType).firstOrNull()?.let { userStat ->
            UserRanking(userStat, 1)
        }
    }

    @Transactional
    fun updateRankingBoard(periodType: PeriodType): RankingBoard {
        val userStats = userStatAdapter.getRanking(RankingCriteria.TOTAL_CALORIES, periodType)
        val rankingBoard = RankingBoard.of(periodType, userStats)
        return rankingBoardAdapter.save(rankingBoard)
    }
}
