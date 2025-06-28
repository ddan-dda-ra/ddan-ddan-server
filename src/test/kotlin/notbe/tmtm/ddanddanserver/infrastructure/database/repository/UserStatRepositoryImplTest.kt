package notbe.tmtm.ddanddanserver.infrastructure.database.repository

import notbe.tmtm.ddanddanserver.domain.model.ranking.PeriodType
import notbe.tmtm.ddanddanserver.domain.model.ranking.RankingCriteria
import org.junit.jupiter.api.Disabled
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.Test

@SpringBootTest
@Disabled
class UserStatRepositoryImplTest {

    @Autowired
    lateinit var userRankingRepository: UserStatRepositoryImpl

    @Test
    fun getUserRanking() {
        val result = userRankingRepository.findAllRankingBy(
            RankingCriteria.TOTAL_SUCCEEDED_DAYS,
            PeriodType.YEARLY
        )

        println("Total Rankings: ${result.size}")

        result.forEachIndexed { index, userRankingEntity ->
            println(userRankingEntity)
        }
    }
}
