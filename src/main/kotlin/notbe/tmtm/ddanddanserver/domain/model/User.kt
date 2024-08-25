package notbe.tmtm.ddanddanserver.domain.model

import notbe.tmtm.ddanddanserver.common.util.generateTsid

class User(
    val id: String,
    var name: String?,
    var purposeCalorie: Int = 100,
) {
    companion object {
        fun register(name: String? = null): User = User(id = generateTsid(), name = name)

        fun create(
            name: String?,
            purposeCalorie: Int,
            feed: Int,
        ): User = User(id = generateTsid(), name = name, purposeCalorie = purposeCalorie, feed = feed)
    }
}
