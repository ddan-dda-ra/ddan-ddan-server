package notbe.tmtm.ddanddanserver.infrastructure.database.repository

import notbe.tmtm.ddanddanserver.domain.exception.UserFoodQuantityLackException
import notbe.tmtm.ddanddanserver.domain.exception.UserNotFoundException
import notbe.tmtm.ddanddanserver.domain.exception.UserTicketLackException
import notbe.tmtm.ddanddanserver.domain.exception.UserToyQuantityLackException
import notbe.tmtm.ddanddanserver.domain.model.user.DeviceToken
import notbe.tmtm.ddanddanserver.domain.model.user.User
import notbe.tmtm.ddanddanserver.domain.model.user.UserSetting
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.findAndModify
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.gt
import org.springframework.data.mongodb.core.query.gte
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.ne
import org.springframework.data.mongodb.core.query.nin
import org.springframework.data.mongodb.core.updateFirst
import org.springframework.data.mongodb.core.updateMulti
import org.springframework.stereotype.Repository
import java.time.LocalDate
import kotlin.reflect.KProperty1

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

    override fun consumeFood(userId: ObjectId, quantity: Int): User =
        decreaseIfEnough(userId, User::foodQuantity, quantity) ?: throw UserFoodQuantityLackException()

    override fun consumeToy(userId: ObjectId, quantity: Int): User =
        decreaseIfEnough(userId, User::toyQuantity, quantity) ?: throw UserToyQuantityLackException()

    override fun increaseItems(userId: ObjectId, food: Int, toy: Int, tickets: Int): User {
        val update = Update()
            .inc(User::foodQuantity.name, food)
            .inc(User::toyQuantity.name, toy)
            .inc(User::tickets.name, tickets)

        return findAndModifyOrThrow(userId, update)
    }

    override fun rewardDailyCalorie(
        userId: ObjectId,
        food: Int,
        purposeAchieved: Boolean,
        lastLoginAt: LocalDate,
    ): User {
        val update = Update()
            .inc(User::foodQuantity.name, food)
            .set(User::lastLoginAt.name, lastLoginAt)
        if (purposeAchieved) {
            update.inc(User::purposeStrict.name, 1)
        }

        return findAndModifyOrThrow(userId, update)
    }

    override fun resetPurposeStrict(userId: ObjectId) {
        val query = Query(User::id isEqualTo userId)
        mongoTemplate.updateFirst<User>(query, Update().set(User::purposeStrict.name, 0))
    }

    override fun resetPurposeStrictExcept(userIds: Collection<ObjectId>): Long {
        val query = Query(
            Criteria().andOperator(
                User::id nin userIds,
                User::purposeStrict ne 0,
            )
        )

        return mongoTemplate.updateMulti<User>(query, Update().set(User::purposeStrict.name, 0)).modifiedCount
    }

    override fun updateProfile(userId: ObjectId, name: String?, purposeCalorie: Int): User {
        val update = Update()
            .set(User::name.name, name)
            .set(User::purposeCalorie.name, purposeCalorie)

        return findAndModifyOrThrow(userId, update)
    }

    override fun updateSetting(userId: ObjectId, setting: UserSetting): User =
        findAndModifyOrThrow(userId, Update().set(User::setting.name, setting))

    override fun updateMainPetId(userId: ObjectId, mainPetId: ObjectId): User =
        findAndModifyOrThrow(userId, Update().set(User::mainPetId.name, mainPetId))

    override fun updateDeviceToken(userId: ObjectId, deviceToken: DeviceToken?) {
        val query = Query(User::id isEqualTo userId)
        mongoTemplate.updateFirst<User>(query, Update().set(User::deviceToken.name, deviceToken?.value))
    }

    private fun decreaseIfEnough(userId: ObjectId, property: KProperty1<User, Int>, quantity: Int): User? {
        val query = Query(
            Criteria().andOperator(
                User::id isEqualTo userId,
                property gte quantity,
            )
        )

        return mongoTemplate.findAndModify<User>(query, Update().inc(property.name, -quantity), RETURN_NEW)
    }

    private fun findAndModifyOrThrow(userId: ObjectId, update: Update): User =
        mongoTemplate.findAndModify<User>(Query(User::id isEqualTo userId), update, RETURN_NEW)
            ?: throw UserNotFoundException("유저를 찾을 수 없습니다. id: $userId")

    companion object {
        private val RETURN_NEW = FindAndModifyOptions.options().returnNew(true)
    }
}
