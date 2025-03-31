package notbe.tmtm.ddanddanserver.domain.usecase.ranking

import notbe.tmtm.ddanddanserver.domain.gateway.RankingBoardGateway
import notbe.tmtm.ddanddanserver.domain.model.ranking.RankingBoard
import notbe.tmtm.ddanddanserver.domain.usecase.UseCase
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UpdateRankingBoard(
    private val rankingBoardGateway: RankingBoardGateway,
) : UseCase<RankingBoard, RankingBoard> {
    @Transactional(readOnly = true)
    override fun execute(input: RankingBoard): RankingBoard = rankingBoardGateway.update(input)
}
