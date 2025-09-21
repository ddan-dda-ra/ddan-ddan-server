package notbe.tmtm.ddanddanserver.infrastructure.database.repository

import notbe.tmtm.ddanddanserver.domain.exception.CheerAlreadyExistsException
import notbe.tmtm.ddanddanserver.domain.model.cheer.Cheer
import org.bson.types.ObjectId
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.gte
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.lte
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class CheerCustomRepositoryImpl(
    private val mongoTemplate: MongoTemplate,
) : CheerCustomRepository {
    override fun findAllByCheereeIdAndThisMonth(
        cheereeId: ObjectId,
    ): List<Cheer> {
        val now = LocalDate.now()
        val startOfMonth = now.withDayOfMonth(1)
        val endOfMonth = now.withDayOfMonth(now.lengthOfMonth())

        val query = Query(
            Criteria().andOperator(
                Cheer::cheereeId isEqualTo cheereeId,
                Cheer::date gte startOfMonth,
                Cheer::date lte endOfMonth,
            )
        )

        return mongoTemplate.find(query, Cheer::class.java)
    }

    override fun saveWithDuplicateCheck(cheer: Cheer): Cheer {
        return try {
            mongoTemplate.save(cheer)
        } catch (e: DuplicateKeyException) {
            throw CheerAlreadyExistsException()
        }
    }
}
