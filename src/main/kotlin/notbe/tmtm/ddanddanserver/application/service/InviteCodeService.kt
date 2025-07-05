package notbe.tmtm.ddanddanserver.application.service

import notbe.tmtm.ddanddanserver.domain.model.friend.InviteCode
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.InviteCodeRepository
import org.bson.types.ObjectId
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class InviteCodeService(
    private val inviteCodeRepository: InviteCodeRepository,
) {
    @Transactional
    fun generateInviteCode(
        inviterId: ObjectId,
        validityHours: Long = 24,
    ): InviteCode {
        val inviteCode =
            InviteCode.create(
                inviterId = inviterId,
                validityHours = validityHours,
            )

        return try {
            inviteCodeRepository.save(inviteCode)
        } catch (e: DuplicateKeyException) {
            generateInviteCode(inviterId, validityHours)
        }
    }
}
