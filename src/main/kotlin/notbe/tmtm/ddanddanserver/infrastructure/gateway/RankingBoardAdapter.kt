package notbe.tmtm.ddanddanserver.infrastructure.gateway

import notbe.tmtm.ddanddanserver.domain.model.ranking.PeriodType
import notbe.tmtm.ddanddanserver.domain.model.ranking.RankingBoard
import notbe.tmtm.ddanddanserver.infrastructure.database.entity.RankingBoardEntity
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.RankingBoardRepository
import org.springframework.stereotype.Component
import kotlin.jvm.optionals.getOrDefault

@Component
class RankingBoardAdapter(
    private val rankingBoardRepository: RankingBoardRepository,
) {
    fun save(rankingBoard: RankingBoard) = rankingBoardRepository.save(RankingBoardEntity.fromDomain(rankingBoard)).toDomain()

    fun getById(id: PeriodType) = rankingBoardRepository.findById(id).getOrDefault(RankingBoardEntity(id, emptyList())).toDomain()

    fun update(rankingBoard: RankingBoard) = rankingBoardRepository.save(RankingBoardEntity.fromDomain(rankingBoard)).toDomain()

    fun delete(id: PeriodType) = rankingBoardRepository.deleteById(id)
}
