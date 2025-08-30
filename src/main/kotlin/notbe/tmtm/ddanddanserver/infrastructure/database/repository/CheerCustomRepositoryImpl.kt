package notbe.tmtm.ddanddanserver.infrastructure.database.repository

import notbe.tmtm.ddanddanserver.domain.exception.CheerAlreadyExistsException
import notbe.tmtm.ddanddanserver.domain.model.cheer.Cheer
import org.bson.types.ObjectId
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.mongodb.core.MongoTemplate
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
    override fun countMonthlyReceivedCheers(
        cheereeId: ObjectId,
    ): Long {
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(29) // 30일간 (오늘 포함)
        
        val query = Query()
            .addCriteria(Cheer::cheereeId isEqualTo cheereeId)
            .addCriteria(Cheer::date gte startDate)
            .addCriteria(Cheer::date lte endDate)

        return mongoTemplate.count(query, Cheer::class.java)
    }
    
    override fun saveWithDuplicateCheck(cheer: Cheer): Cheer {
        return try {
            mongoTemplate.save(cheer)
        } catch (e: DuplicateKeyException) {
            throw CheerAlreadyExistsException()
        }
    }
}
