package notbe.tmtm.ddanddanserver.domain.gateway

import notbe.tmtm.ddanddanserver.domain.model.ranking.PeriodType
import notbe.tmtm.ddanddanserver.domain.model.ranking.RankingBoard

interface RankingBoardGateway {
    fun save(rankingBoard: RankingBoard): RankingBoard

    fun getById(id: PeriodType): RankingBoard

    fun update(rankingBoard: RankingBoard): RankingBoard

    fun delete(id: PeriodType)
}
