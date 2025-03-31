package notbe.tmtm.ddanddanserver.infrastructure.gateway

import notbe.tmtm.ddanddanserver.domain.gateway.RankingBoardGateway
import notbe.tmtm.ddanddanserver.domain.model.ranking.PeriodType
import notbe.tmtm.ddanddanserver.domain.model.ranking.RankingBoard
import notbe.tmtm.ddanddanserver.infrastructure.database.entity.RankingBoardEntity
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.RankingBoardRepository
import org.springframework.stereotype.Component
import kotlin.jvm.optionals.getOrDefault

@Component
class RankingBoardGatewayImpl(
    private val rankingBoardRepository: RankingBoardRepository,
) : RankingBoardGateway {
    override fun save(rankingBoard: RankingBoard) = rankingBoardRepository.save(RankingBoardEntity.fromDomain(rankingBoard)).toDomain()

    override fun getById(id: PeriodType) = rankingBoardRepository.findById(id).getOrDefault(RankingBoardEntity(id, emptyList())).toDomain()

    override fun update(rankingBoard: RankingBoard) = rankingBoardRepository.save(RankingBoardEntity.fromDomain(rankingBoard)).toDomain()

    override fun delete(id: PeriodType) = rankingBoardRepository.deleteById(id)
}
