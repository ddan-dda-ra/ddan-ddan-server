package notbe.tmtm.ddanddanserver.infrastructure.database.repository

import notbe.tmtm.ddanddanserver.domain.exception.UserNotFoundException
import notbe.tmtm.ddanddanserver.domain.exception.UserTicketLackException
import notbe.tmtm.ddanddanserver.domain.model.user.User
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.gt
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.updateFirst
import org.springframework.stereotype.Repository

@Repository
class UserRepositoryCustomImpl(
    private val mongoTemplate: MongoTemplate,
) : UserRepositoryCustom {
    override fun increaseTickets(userId: ObjectId, amount: Int) {
        val query = Query().addCriteria(
            User::id isEqualTo userId,
        )
        val update = Update().inc("tickets", amount)

        mongoTemplate.updateFirst<User>(query, update).let {
            if (it.matchedCount == 0L) {
                throw UserNotFoundException("유저를 찾을 수 없습니다. id: $userId")
            }
        }
    }

    override fun decreaseTickets(userId: ObjectId, amount: Int) {
        val query = Query(
            Criteria().andOperator(
                User::id isEqualTo userId,
                User::tickets gt 0,
            )
        )
        val update = Update().inc("tickets", -amount)

        mongoTemplate.updateFirst<User>(query, update).let {
            if (it.matchedCount == 0L) {
                throw UserTicketLackException("티켓이 부족합니다. id: $userId")
            }
        }
    }
}
