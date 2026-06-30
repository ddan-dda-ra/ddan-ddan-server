package notbe.tmtm.ddanddanserver.migration

import notbe.tmtm.ddanddanserver.infrastructure.database.entity.PetCatalogEntity
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import java.time.Instant

@SpringBootTest
@EnabledIfEnvironmentVariable(
    named = "PET_CATALOG_BACKGROUNDS_MIGRATION_CONFIRM",
    matches = "NEW_VERSION_DEPLOYED_API_SMOKED_BACKUP_COMPLETED",
)
class PetCatalogBackgroundsRemovalMigration @Autowired constructor(
    private val mongoTemplate: MongoTemplate,
) {
    @Test
    fun migrate() {
        val migrationStartedAt = Instant.now()
        val targetQuery = Query(Criteria.where("backgrounds").exists(true))
        val totalBefore = mongoTemplate.count(Query(), PetCatalogEntity::class.java)
        val targetBefore = mongoTemplate.count(targetQuery, PetCatalogEntity::class.java)
        val expectedTotal = requiredExpectedCount(EXPECTED_TOTAL_ENV)
        val expectedTarget = requiredExpectedCount(EXPECTED_TARGET_ENV)

        check(totalBefore == expectedTotal) {
            "전체 문서 수가 확인값과 다릅니다: actual=$totalBefore, expected=$expectedTotal"
        }
        check(targetBefore == expectedTarget) {
            "backgrounds 대상 문서 수가 확인값과 다릅니다: actual=$targetBefore, expected=$expectedTarget"
        }

        logger.info(
            "Pet catalog backgrounds migration started: totalBefore={}, targetBefore={}, migrationStartedAt={}",
            totalBefore,
            targetBefore,
            migrationStartedAt,
        )

        val result =
            mongoTemplate.updateMulti(
                targetQuery,
                Update().unset("backgrounds").set("updatedAt", migrationStartedAt),
                PetCatalogEntity::class.java,
            )

        val remaining = mongoTemplate.count(targetQuery, PetCatalogEntity::class.java)
        val totalAfter = mongoTemplate.count(Query(), PetCatalogEntity::class.java)
        val updatedAtCount =
            mongoTemplate.count(
                Query(Criteria.where("updatedAt").`is`(migrationStartedAt)),
                PetCatalogEntity::class.java,
            )

        logger.info(
            "Pet catalog backgrounds migration finished: matchedCount={}, modifiedCount={}, remaining={}, totalAfter={}, updatedAtCount={}",
            result.matchedCount,
            result.modifiedCount,
            remaining,
            totalAfter,
            updatedAtCount,
        )

        check(result.matchedCount == targetBefore) {
            "matchedCount(${result.matchedCount}) != targetBefore($targetBefore)"
        }
        check(result.modifiedCount == result.matchedCount) {
            "modifiedCount(${result.modifiedCount}) != matchedCount(${result.matchedCount})"
        }
        check(remaining == 0L) {
            "backgrounds 필드가 남은 문서가 있습니다: $remaining"
        }
        check(totalAfter == totalBefore) {
            "전체 문서 수가 변경되었습니다: before=$totalBefore, after=$totalAfter"
        }
        if (result.modifiedCount > 0L) {
            check(updatedAtCount == result.modifiedCount) {
                "updatedAt 매핑 또는 갱신 결과가 일치하지 않습니다: updatedAtCount=$updatedAtCount, modifiedCount=${result.modifiedCount}"
            }
        }
    }

    companion object {
        private const val EXPECTED_TOTAL_ENV = "PET_CATALOG_MIGRATION_EXPECTED_TOTAL"
        private const val EXPECTED_TARGET_ENV = "PET_CATALOG_MIGRATION_EXPECTED_TARGET"
        private val logger = LoggerFactory.getLogger(PetCatalogBackgroundsRemovalMigration::class.java)

        private fun requiredExpectedCount(name: String): Long =
            requireNotNull(System.getenv(name)?.toLongOrNull()) {
                "$name 환경변수에 실행 전 확인한 문서 수를 입력해야 합니다."
            }
    }
}
