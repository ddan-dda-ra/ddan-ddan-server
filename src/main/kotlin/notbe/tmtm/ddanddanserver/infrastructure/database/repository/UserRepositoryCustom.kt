package notbe.tmtm.ddanddanserver.infrastructure.database.repository

import org.bson.types.ObjectId

interface UserRepositoryCustom {
    fun increaseTickets(userId: ObjectId, amount: Int)

    fun decreaseTickets(userId: ObjectId, amount: Int)
}
