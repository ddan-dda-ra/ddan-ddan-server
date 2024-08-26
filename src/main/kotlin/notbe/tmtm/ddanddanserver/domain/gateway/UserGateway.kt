package notbe.tmtm.ddanddanserver.domain.gateway

import notbe.tmtm.ddanddanserver.domain.model.User

interface UserGateway {
    fun save(user: User): User

    fun getById(userId: String): User

    fun update(user: User): User
}
