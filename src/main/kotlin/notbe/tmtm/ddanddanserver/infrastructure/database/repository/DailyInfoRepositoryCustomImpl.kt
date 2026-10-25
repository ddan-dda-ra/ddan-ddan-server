package notbe.tmtm.ddanddanserver.infrastructure.database.repository

import notbe.tmtm.ddanddanserver.domain.model.user.DailyInfo
import org.bson.types.ObjectId
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.findAndModify
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.ne
import org.springframework.data.mongodb.core.updateFirst
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class DailyInfoRepositoryCustomImpl(
    private val mongoTemplate: MongoTemplate,
) : DailyInfoRepositoryCustom {
    override fun raiseCalorie(id: ObjectId, calorie: Int): Int {
        val previous = mongoTemplate.findAndModify<DailyInfo>(
            Query(DailyInfo::id isEqualTo id),
            Update().max(DailyInfo::calorie.name, calorie),
            FindAndModifyOptions.options().returnNew(false),
        ) ?: throw EmptyResultDataAccessException("DailyInfo not found with id: $id", 1)

        return previous.calorie
    }

    override fun revertCalorie(id: ObjectId, from: Int, to: Int) {
        val query = Query(
            Criteria().andOperator(
                DailyInfo::id isEqualTo id,
                DailyInfo::calorie isEqualTo from,
            )
        )
        mongoTemplate.updateFirst<DailyInfo>(query, Update().set(DailyInfo::calorie.name, to))
    }

    override fun markPurposeAchieved(id: ObjectId): Boolean {
        val query = Query(
            Criteria().andOperator(
                DailyInfo::id isEqualTo id,
                DailyInfo::purposeAchieved ne true,
            )
        )

        return mongoTemplate.updateFirst<DailyInfo>(query, Update().set(DailyInfo::purposeAchieved.name, true))
            .modifiedCount == 1L
    }

    override fun unmarkPurposeAchieved(id: ObjectId) {
        setById(id, Update().set(DailyInfo::purposeAchieved.name, false))
    }

    override fun markToyGiven(id: ObjectId) {
        setById(id, Update().set(DailyInfo::toyGiven.name, true))
    }

    override fun updateUserName(userId: ObjectId, date: LocalDate, userName: String?) {
        setByUserIdAndDate(userId, date, Update().set(DailyInfo::userName.name, userName))
    }

    override fun updatePetType(userId: ObjectId, date: LocalDate, petType: String?) {
        setByUserIdAndDate(userId, date, Update().set(DailyInfo::petType.name, petType))
    }

    private fun setById(id: ObjectId, update: Update) {
        mongoTemplate.updateFirst<DailyInfo>(Query(DailyInfo::id isEqualTo id), update)
    }

    private fun setByUserIdAndDate(userId: ObjectId, date: LocalDate, update: Update) {
        val query = Query(
            Criteria().andOperator(
                DailyInfo::userId isEqualTo userId,
                DailyInfo::date isEqualTo date,
            )
        )
        mongoTemplate.updateFirst<DailyInfo>(query, update)
    }
}
