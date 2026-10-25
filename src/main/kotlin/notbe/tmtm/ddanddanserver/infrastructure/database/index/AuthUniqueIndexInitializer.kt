package notbe.tmtm.ddanddanserver.infrastructure.database.index

import jakarta.annotation.PostConstruct
import notbe.tmtm.ddanddanserver.common.util.logger
import notbe.tmtm.ddanddanserver.domain.model.auth.Auth
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.Index
import org.springframework.stereotype.Component

/**
 * auths에 (type, o_auth_id) unique 인덱스를 만든다.
 *
 * 같은 OAuth 계정으로 동시에 가입 요청이 들어와도 Auth가 하나만 저장되게 한다.
 * `@CompoundIndex`로 선언하지 않은 이유: 기존 데이터에 중복이 있으면 인덱스 생성이 실패하는데,
 * 자동 인덱스 생성은 이 실패로 앱 기동 자체를 막는다. 여기서는 실패해도 error 로그만 남기고 기동은 계속한다.
 * 기존에 수동으로 만든 비 unique 인덱스(o_auth_id_1_type_1)와 키 순서가 달라서 함께 둘 수 있다.
 */
@Component
class AuthUniqueIndexInitializer(
    private val mongoTemplate: MongoTemplate,
) {
    private val log = logger()

    @PostConstruct
    fun ensureIndex() {
        runCatching {
            mongoTemplate.indexOps(Auth::class.java).ensureIndex(
                Index()
                    .on("type", Sort.Direction.ASC)
                    .on("o_auth_id", Sort.Direction.ASC)
                    .unique()
                    .named(INDEX_NAME),
            )
        }.onFailure {
            log.error("auths unique 인덱스($INDEX_NAME) 생성 실패. 중복된 (type, o_auth_id)를 정리한 뒤 재시작해야 합니다.", it)
        }
    }

    companion object {
        const val INDEX_NAME = "type_o_auth_id_unique"
    }
}
