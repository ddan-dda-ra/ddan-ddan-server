package notbe.tmtm.ddanddanserver.infrastructure.database.seed

import jakarta.annotation.PostConstruct
import notbe.tmtm.ddanddanserver.common.util.logger
import notbe.tmtm.ddanddanserver.infrastructure.database.entity.PetCatalogBackgroundsEntity
import notbe.tmtm.ddanddanserver.infrastructure.database.entity.PetCatalogEntity
import notbe.tmtm.ddanddanserver.infrastructure.database.entity.PetCatalogLevelEntity
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.PetCatalogRepository
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class PetCatalogSeeder(
    private val repository: PetCatalogRepository,
) {
    @PostConstruct
    fun seed() {
        if (repository.count() > 0L) return
        val now = Instant.now()
        val entities =
            DEFAULT_PETS.mapIndexed { index, pet ->
                PetCatalogEntity(
                    type = pet.type,
                    name = pet.name,
                    backgrounds = buildBackgrounds(pet.species),
                    isActive = true,
                    displayOrder = index,
                    levels = buildLevels(pet.species),
                    createdAt = now,
                    updatedAt = now,
                )
            }
        try {
            repository.saveAll(entities)
            logger().info("PetCatalog seeded with {} default pets", entities.size)
        } catch (e: DuplicateKeyException) {
            // 다중 인스턴스 동시 startup 시 다른 인스턴스가 먼저 seed한 경우
            logger().info("PetCatalog seed skipped: 이미 다른 인스턴스가 시드 데이터를 등록했습니다", e)
        }
    }

    private fun buildBackgrounds(species: String): PetCatalogBackgroundsEntity =
        PetCatalogBackgroundsEntity(
            home = "$CDN_BASE/$species/backgrounds/home.png?v=$INITIAL_VERSION",
            homeCompact = "$CDN_BASE/$species/backgrounds/home_compact.png?v=$INITIAL_VERSION",
            friendCard = "$CDN_BASE/$species/backgrounds/friend_card.png?v=$INITIAL_VERSION",
        )

    private fun buildLevels(species: String): Map<Int, PetCatalogLevelEntity> =
        (1..MAX_LEVEL).associateWith { level ->
            PetCatalogLevelEntity(
                imageUrl = "$CDN_BASE/$species/level$level.png?v=$INITIAL_VERSION",
                lottieDefaultUrl = "$CDN_BASE/$species/level${level}_default.json?v=$INITIAL_VERSION",
                lottiePlayEatUrl = "$CDN_BASE/$species/level${level}_play_eat.json?v=$INITIAL_VERSION",
            )
        }

    private data class PetSeed(
        val type: String,
        val name: String,
        val species: String,
    )

    companion object {
        private const val CDN_BASE = "https://ddan-ddan-cdn.ddmz.org"
        private const val MAX_LEVEL = 5

        // 자산 캐시 무효화용 버전 쿼리. R2의 자산을 교체할 때마다 어드민에서 URL을 갱신해 v를 증분.
        private const val INITIAL_VERSION = 1
        private val DEFAULT_PETS =
            listOf(
                PetSeed(type = "CAT", name = "고양이", species = "cat"),
                PetSeed(type = "HAMSTER", name = "햄스터", species = "hamster"),
                PetSeed(type = "PENGUIN", name = "펭귄", species = "penguin"),
                PetSeed(type = "DOG", name = "강아지", species = "dog"),
                PetSeed(type = "MOLE", name = "두더지", species = "mole"),
            )
    }
}
