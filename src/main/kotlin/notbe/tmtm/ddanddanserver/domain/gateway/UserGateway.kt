package notbe.tmtm.ddanddanserver.domain.gateway

import notbe.tmtm.ddanddanserver.domain.model.user.User

interface UserGateway {
    fun save(user: User): User

    fun findAll(): List<User>

    fun getById(userId: String): User

    fun update(user: User): User

    fun delete(userId: String)
}
