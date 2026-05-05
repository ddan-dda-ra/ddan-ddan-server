package notbe.tmtm.ddanddanserver.application.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import notbe.tmtm.ddanddanserver.domain.exception.UserNotFoundException
import notbe.tmtm.ddanddanserver.domain.model.user.DeviceToken
import notbe.tmtm.ddanddanserver.domain.model.user.User
import notbe.tmtm.ddanddanserver.domain.model.user.UserSetting
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserRepository
import org.bson.types.ObjectId
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

class UserAdminServiceTest : FunSpec({
    lateinit var repository: UserRepository
    lateinit var service: UserAdminService

    fun user(name: String?): User =
        User(
            id = ObjectId(),
            deviceToken = DeviceToken("token"),
            name = name,
            setting = UserSetting(),
        )

    beforeEach {
        repository = mockk()
        service = UserAdminService(repository)
    }

    test("keyword가 null이면 findAll(pageable)을 호출한다") {
        val pageable = PageRequest.of(0, 10)
        every { repository.findAll(pageable) } returns PageImpl(listOf(user("ddingmin")))

        service.searchUsers(null, pageable)

        verify(exactly = 1) { repository.findAll(pageable) }
        verify(exactly = 0) { repository.findByNameContainingIgnoreCase(any(), any()) }
    }

    test("keyword가 빈 문자열이어도 findAll(pageable)을 호출한다") {
        val pageable = PageRequest.of(0, 10)
        every { repository.findAll(pageable) } returns PageImpl(emptyList())

        service.searchUsers("", pageable)

        verify(exactly = 1) { repository.findAll(pageable) }
    }

    test("keyword가 있으면 findByNameContainingIgnoreCase를 호출한다") {
        val pageable = PageRequest.of(0, 10)
        every { repository.findByNameContainingIgnoreCase("딴딴", pageable) } returns PageImpl(listOf(user("딴딴")))

        service.searchUsers("딴딴", pageable)

        verify(exactly = 1) { repository.findByNameContainingIgnoreCase("딴딴", pageable) }
        verify(exactly = 0) { repository.findAll(any<PageRequest>()) }
    }

    test("getUser는 ObjectId로 사용자를 조회한다") {
        val id = ObjectId()
        val expected = user("ddingmin")
        every { repository.findById(id) } returns java.util.Optional.of(expected)

        val result = service.getUser(id)

        result shouldBe expected
    }

    test("getUser가 존재하지 않으면 UserNotFoundException") {
        val id = ObjectId()
        every { repository.findById(id) } returns java.util.Optional.empty()

        shouldThrow<UserNotFoundException> {
            service.getUser(id)
        }
    }
})
