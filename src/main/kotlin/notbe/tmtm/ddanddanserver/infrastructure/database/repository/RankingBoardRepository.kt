package notbe.tmtm.ddanddanserver.infrastructure.database.repository

import notbe.tmtm.ddanddanserver.domain.model.ranking.PeriodType
import notbe.tmtm.ddanddanserver.domain.model.ranking.RankingBoard
import notbe.tmtm.ddanddanserver.infrastructure.database.entity.RankingBoardEntity
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.repository.findByIdOrNull

interface RankingBoardRepository : MongoRepository<RankingBoardEntity, PeriodType>

fun RankingBoardRepository.findByIdOrDefault(id: PeriodType): RankingBoard {
    return (findByIdOrNull(id) ?: RankingBoardEntity(id, emptyList())).toDomain()
}

fun RankingBoardRepository.save(rankingBoard: RankingBoard): RankingBoard {
    return save(RankingBoardEntity.fromDomain(rankingBoard)).toDomain()
}
