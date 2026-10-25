package notbe.tmtm.ddanddanserver.infrastructure.database.repository

import org.bson.types.ObjectId
import java.time.LocalDate

/**
 * DailyInfo 문서를 필드 단위로 원자적으로 갱신한다.
 * 문서 전체 save()는 동시 요청의 칼로리 갱신을 덮어쓰므로, 생성 외에는 이 메서드들로만 갱신한다.
 */
interface DailyInfoRepositoryCustom {
    /** 칼로리를 기존 값과 [calorie] 중 큰 값으로 올리고, 갱신 전 칼로리를 반환한다. */
    fun raiseCalorie(id: ObjectId, calorie: Int): Int

    /** 칼로리가 아직 [from]일 때만 [to]로 되돌린다. 그 사이 다른 요청이 올렸다면 건드리지 않는다. */
    fun revertCalorie(id: ObjectId, from: Int, to: Int)

    /** 아직 목표 달성 처리가 안 됐을 때만 달성으로 바꾼다. 이번 호출이 바꿨으면 true. */
    fun markPurposeAchieved(id: ObjectId): Boolean

    fun unmarkPurposeAchieved(id: ObjectId)

    fun markToyGiven(id: ObjectId)

    fun updateUserName(userId: ObjectId, date: LocalDate, userName: String?)

    fun updatePetType(userId: ObjectId, date: LocalDate, petType: String?)
}
