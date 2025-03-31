package notbe.tmtm.ddanddanserver.domain.usecase.ranking

import notbe.tmtm.ddanddanserver.domain.gateway.RankingBoardGateway
import notbe.tmtm.ddanddanserver.domain.model.ranking.PeriodType
import notbe.tmtm.ddanddanserver.domain.model.ranking.RankingBoard
import notbe.tmtm.ddanddanserver.domain.usecase.UseCase
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GetRankingBoard(
    private val rankingBoardGateway: RankingBoardGateway
) : UseCase<PeriodType, RankingBoard> {

    @Transactional(readOnly = true)
    override fun execute(input: PeriodType): RankingBoard {
        return rankingBoardGateway.getById(input)
    }
}
