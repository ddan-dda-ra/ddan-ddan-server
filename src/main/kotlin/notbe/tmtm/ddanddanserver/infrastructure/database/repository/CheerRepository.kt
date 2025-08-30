package notbe.tmtm.ddanddanserver.infrastructure.database.repository

import notbe.tmtm.ddanddanserver.domain.model.cheer.Cheer
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface CheerRepository : MongoRepository<Cheer, ObjectId>, CheerCustomRepository {
}
