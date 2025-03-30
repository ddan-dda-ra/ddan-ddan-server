package notbe.tmtm.ddanddanserver.domain.usecase.ranking

import notbe.tmtm.ddanddanserver.domain.gateway.UserStatGateway
import notbe.tmtm.ddanddanserver.domain.model.ranking.PeriodType
import notbe.tmtm.ddanddanserver.domain.model.ranking.RankingCriteria
import notbe.tmtm.ddanddanserver.domain.model.ranking.UserStat
import notbe.tmtm.ddanddanserver.domain.usecase.UseCase
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GetTopRanking(
    private val userStatGateway: UserStatGateway,
) : UseCase<GetTopRanking.Input, GetTopRanking.Output> {
    data class Input(
        val criteria: RankingCriteria,
        val periodType: PeriodType,
    )

    data class Output(
        val user: UserStat?,
    )

    @Transactional(readOnly = true)
    override fun execute(input: Input): Output {
        val userStats = userStatGateway.getRanking(input.criteria, input.periodType)

        if (userStats.isEmpty()) {
            return Output(null)
        }

        return Output(userStats.first())
    }
}
