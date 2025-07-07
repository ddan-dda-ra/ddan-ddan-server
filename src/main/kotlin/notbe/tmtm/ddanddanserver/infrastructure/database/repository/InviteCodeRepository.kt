package notbe.tmtm.ddanddanserver.infrastructure.database.repository

import notbe.tmtm.ddanddanserver.domain.exception.InviteCodeNotFoundException
import notbe.tmtm.ddanddanserver.domain.model.friend.InviteCode
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.repository.findByIdOrNull

interface InviteCodeRepository : MongoRepository<InviteCode, ObjectId> {
    fun findByCode(code: String): InviteCode?

    fun deleteAllByInviterId(inviterId: ObjectId)
}

fun InviteCodeRepository.findByIdOrThrow(inviteCodeId: ObjectId): InviteCode =
    findByIdOrNull(inviteCodeId)
        ?: throw InviteCodeNotFoundException()

fun InviteCodeRepository.findByCodeOrThrow(code: String): InviteCode =
    findByCode(code)
        ?: throw InviteCodeNotFoundException()
