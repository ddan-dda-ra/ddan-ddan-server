package notbe.tmtm.ddanddanserver.infrastructure.database.repository

import notbe.tmtm.ddanddanserver.domain.model.ranking.PeriodType
import notbe.tmtm.ddanddanserver.infrastructure.database.entity.RankingBoardEntity
import org.springframework.data.mongodb.repository.MongoRepository

interface RankingBoardRepository : MongoRepository<RankingBoardEntity, PeriodType>
