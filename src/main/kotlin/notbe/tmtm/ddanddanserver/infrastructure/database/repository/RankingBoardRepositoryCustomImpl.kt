package notbe.tmtm.ddanddanserver.infrastructure.database.repository

import notbe.tmtm.ddanddanserver.infrastructure.database.entity.RankingBoardEntity
import org.bson.Document
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.updateMulti
import org.springframework.stereotype.Repository

@Repository
class RankingBoardRepositoryCustomImpl(
    private val mongoTemplate: MongoTemplate,
) : RankingBoardRepositoryCustom {
    override fun removeUser(userId: ObjectId) {
        // ranking은 Pair(순위, UserStatEntity) 목록이라 저장 형태가 {first, second: {user: {_id}}}이다.
        mongoTemplate.updateMulti<RankingBoardEntity>(
            Query(),
            Update().pull(RankingBoardEntity::ranking.name, Document("second.user._id", userId)),
        )
    }
}
