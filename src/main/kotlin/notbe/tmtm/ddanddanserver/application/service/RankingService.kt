package notbe.tmtm.ddanddanserver.application.service

import notbe.tmtm.ddanddanserver.domain.model.ranking.*
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.*
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private const val RANKING_LIMIT = 100

@Service
class RankingService(
    private val userStatRepository: UserStatRepository,
    private val rankingBoardRepository: RankingBoardRepository,
) {
    @Transactional(readOnly = true)
    fun getRanking(
        userId: ObjectId,
        criteria: RankingCriteria,
        periodType: PeriodType,
    ): RankingResult {
        val userStats = userStatRepository.getAllRankingBy(criteria, periodType)
        return RankingResult.create(userStats, criteria, userId)
    }

    @Transactional(readOnly = true)
    fun getTopRanking(criteria: RankingCriteria, periodType: PeriodType): UserStat? {
        val userStats: List<UserStat> = userStatRepository.getAllRankingBy(criteria, periodType)
        return if (userStats.isEmpty()) null else userStats.first()
    }

    @Transactional(readOnly = true)
    fun getRankingBoard(periodType: PeriodType): RankingBoard {
        return rankingBoardRepository.findByIdOrDefault(periodType)
    }

    @Transactional
    fun updateRankingBoard(rankingBoard: RankingBoard): RankingBoard {
        return rankingBoardRepository.save(rankingBoard)
    }
}
