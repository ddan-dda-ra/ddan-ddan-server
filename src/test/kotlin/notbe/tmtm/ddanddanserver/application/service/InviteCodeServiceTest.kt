package notbe.tmtm.ddanddanserver.application.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import notbe.tmtm.ddanddanserver.domain.model.friend.InviteCode
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.InviteCodeRepository
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class InviteCodeServiceTest {
    private lateinit var inviteCodeRepository: InviteCodeRepository
    private lateinit var inviteCodeService: InviteCodeService

    @BeforeEach
    fun setUp() {
        inviteCodeRepository = mockk()
        inviteCodeService = InviteCodeService(inviteCodeRepository)
    }

    @Test
    fun `초대코드를 생성할 수 있다`() {
        // given
        val inviterId = ObjectId()
        val inviteCode = createTestInviteCode(inviterId)

        every { inviteCodeRepository.save(any()) } returns inviteCode

        // when
        val result = inviteCodeService.generateInviteCode(inviterId)

        // then
        assertEquals(inviteCode, result)
        verify { inviteCodeRepository.save(any()) }
    }

    private fun createTestInviteCode(
        inviterId: ObjectId,
        expiredHours: Long = 24,
    ): InviteCode =
        InviteCode.create(inviterId, expiredHours).apply {
            // MockK를 사용하여 private 필드들을 모킹
        }
}
