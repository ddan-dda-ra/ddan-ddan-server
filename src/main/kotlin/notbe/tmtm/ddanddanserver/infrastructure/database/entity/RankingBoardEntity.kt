package notbe.tmtm.ddanddanserver.infrastructure.database.entity

import notbe.tmtm.ddanddanserver.domain.model.ranking.PeriodType
import notbe.tmtm.ddanddanserver.domain.model.ranking.RankingBoard

data class RankingBoardEntity(
    val id: PeriodType,
    val ranking: List<Pair<Int, UserStatEntity>>
) {
    companion object {
        fun fromDomain(rankingBoard: RankingBoard): RankingBoardEntity =
            RankingBoardEntity(
                id = rankingBoard.id,
                ranking = rankingBoard.ranking.map { it.first to UserStatEntity.fromDomain(it.second) }
            )
    }

    fun toDomain(): RankingBoard =
        RankingBoard(
            id = id,
            ranking = ranking.map { it.first to it.second.toDomain() }
        )
}
