package notbe.tmtm.ddanddanserver.infrastructure.database.repository

import notbe.tmtm.ddanddanserver.domain.model.cheer.Cheer
import org.bson.types.ObjectId

interface CheerCustomRepository {
    fun countMonthlyReceivedCheers(cheereeId: ObjectId): Long
    
    fun saveWithDuplicateCheck(cheer: Cheer): Cheer
}
