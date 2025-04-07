package notbe.tmtm.ddanddanserver.domain.gateway

import notbe.tmtm.ddanddanserver.domain.model.user.User
import org.bson.types.ObjectId

interface UserGateway {
    fun save(user: User): User

    fun findAll(): List<User>

    fun getById(userId: ObjectId): User

    fun update(user: User): User

    fun delete(userId: ObjectId)
}
