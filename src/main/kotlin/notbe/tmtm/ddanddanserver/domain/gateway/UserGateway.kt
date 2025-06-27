package notbe.tmtm.ddanddanserver.domain.gateway

import notbe.tmtm.ddanddanserver.domain.model.user.User
import org.bson.types.ObjectId

interface UserGateway {
    fun getById(id: ObjectId): User
    fun save(user: User): User
    fun update(user: User): User
    fun findAll(): List<User>
}