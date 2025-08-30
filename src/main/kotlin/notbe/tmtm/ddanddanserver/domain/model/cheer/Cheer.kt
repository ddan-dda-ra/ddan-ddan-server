package notbe.tmtm.ddanddanserver.domain.model.cheer

import notbe.tmtm.ddanddanserver.domain.exception.CheerSelfException
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDate
import java.time.LocalDateTime

@Document("cheers")
@CompoundIndex(name = "cheerer_cheeree_date_unique", def = "{'cheererId': 1, 'cheereeId': 1, 'date': 1}", unique = true)
@CompoundIndex(name = "cheeree_date_range", def = "{'cheereeId': 1, 'date': -1}")
@CompoundIndex(name = "cheerer_date", def = "{'cheererId': 1, 'date': 1}")
class Cheer private constructor(
    val id: ObjectId,
    val cheererId: ObjectId,
    val cheereeId: ObjectId,
    val date: LocalDate,
    val createdAt: LocalDateTime,
) {
    init {
        if (cheererId == cheereeId) {
            throw CheerSelfException()
        }
    }

    companion object {
        fun create(
            cheererId: ObjectId,
            cheereeId: ObjectId,
            date: LocalDate = LocalDate.now(),
        ): Cheer {
            return Cheer(
                id = ObjectId(),
                cheererId = cheererId,
                cheereeId = cheereeId,
                date = date,
                createdAt = LocalDateTime.now(),
            )
        }
    }

    fun isCheerFor(cheereeId: ObjectId): Boolean = this.cheereeId == cheereeId

    fun isCheerBy(cheererId: ObjectId): Boolean = this.cheererId == cheererId

    fun isCheerBetween(userId1: ObjectId, userId2: ObjectId): Boolean =
        (cheererId == userId1 && cheereeId == userId2) || (cheererId == userId2 && cheereeId == userId1)
}
