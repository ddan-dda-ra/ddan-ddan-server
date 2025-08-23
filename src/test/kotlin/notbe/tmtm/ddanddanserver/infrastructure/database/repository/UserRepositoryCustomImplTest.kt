package notbe.tmtm.ddanddanserver.infrastructure.database.repository

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import notbe.tmtm.ddanddanserver.domain.exception.UserNotFoundException
import notbe.tmtm.ddanddanserver.domain.exception.UserTicketLackException
import notbe.tmtm.ddanddanserver.domain.model.user.User
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import com.mongodb.client.result.UpdateResult

class UserRepositoryCustomImplTest : FunSpec({
    lateinit var mongoTemplate: MongoTemplate
    lateinit var userRepositoryCustomImpl: UserRepositoryCustomImpl

    beforeEach {
        mongoTemplate = mockk()
        userRepositoryCustomImpl = UserRepositoryCustomImpl(mongoTemplate)
    }

    context("increaseTickets") {
        test("사용자가 존재할 때 티켓을 증가시켜야 한다") {
            val userId = ObjectId()
            val amount = 5
            val updateResult = mockk<UpdateResult>()

            every { updateResult.matchedCount } returns 1L
            every { mongoTemplate.updateFirst(any<Query>(), any<Update>(), User::class.java) } returns updateResult

            userRepositoryCustomImpl.increaseTickets(userId, amount)

            verify { mongoTemplate.updateFirst(any<Query>(), any<Update>(), User::class.java) }
        }

        test("사용자가 존재하지 않을 때 UserNotFoundException을 발생시켜야 한다") {
            val userId = ObjectId()
            val amount = 5
            val updateResult = mockk<UpdateResult>()

            every { updateResult.matchedCount } returns 0L
            every { mongoTemplate.updateFirst(any<Query>(), any<Update>(), User::class.java) } returns updateResult

            shouldThrow<UserNotFoundException> {
                userRepositoryCustomImpl.increaseTickets(userId, amount)
            }
        }
    }

    context("decreaseTickets") {
        test("사용자가 존재하고 티켓이 충분할 때 티켓을 감소시켜야 한다") {
            val userId = ObjectId()
            val amount = 3
            val updateResult = mockk<UpdateResult>()

            every { updateResult.matchedCount } returns 1L
            every { mongoTemplate.updateFirst(any<Query>(), any<Update>(), User::class.java) } returns updateResult

            userRepositoryCustomImpl.decreaseTickets(userId, amount)

            verify { mongoTemplate.updateFirst(any<Query>(), any<Update>(), User::class.java) }
        }

        test("사용자가 존재하지 않거나 티켓이 부족할 때 UserTicketLackException을 발생시켜야 한다") {
            val userId = ObjectId()
            val amount = 5
            val updateResult = mockk<UpdateResult>()

            every { updateResult.matchedCount } returns 0L
            every { mongoTemplate.updateFirst(any<Query>(), any<Update>(), User::class.java) } returns updateResult

            shouldThrow<UserTicketLackException> {
                userRepositoryCustomImpl.decreaseTickets(userId, amount)
            }
        }
    }
})
