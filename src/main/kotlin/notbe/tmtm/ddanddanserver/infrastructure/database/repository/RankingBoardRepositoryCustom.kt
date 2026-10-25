package notbe.tmtm.ddanddanserver.infrastructure.database.repository

import org.bson.types.ObjectId

interface RankingBoardRepositoryCustom {
    /** 모든 랭킹보드에서 해당 유저 항목을 제거한다. */
    fun removeUser(userId: ObjectId)
}
