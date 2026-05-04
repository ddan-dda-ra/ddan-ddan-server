package notbe.tmtm.ddanddanserver.infrastructure.database.seed

import jakarta.annotation.PostConstruct
import notbe.tmtm.ddanddanserver.common.util.logger
import notbe.tmtm.ddanddanserver.infrastructure.database.entity.PetCatalogEntity
import notbe.tmtm.ddanddanserver.infrastructure.database.entity.PetCatalogLevelEntity
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.PetCatalogRepository
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
                    key = pet.key,
                    name = pet.name,
                    isActive = true,
                    displayOrder = index,
                    levels = buildLevels(pet.species),
                    createdAt = now,
                    updatedAt = now,
                )
            }
        repository.saveAll(entities)
        logger().info("PetCatalog seeded with {} default pets", entities.size)
    }

    private fun buildLevels(species: String): Map<Int, PetCatalogLevelEntity> =
        (1..MAX_LEVEL).associateWith { level ->
            PetCatalogLevelEntity(
                imageUrl = "$CDN_BASE/${species}_level$level.png",
                lottieDefaultUrl = "$CDN_BASE/${species}_level${level}_default.json",
                lottiePlayEatUrl = "$CDN_BASE/${species}_level${level}_play_eat.json",
            )
        }

    private data class PetSeed(
        val key: String,
        val name: String,
        val species: String,
    )

    companion object {
        private const val CDN_BASE = "https://ddan-ddan-cdn.ddmz.org"
        private const val MAX_LEVEL = 5
        private val DEFAULT_PETS =
            listOf(
                PetSeed(key = "CAT", name = "고양이", species = "cat"),
                PetSeed(key = "HAMSTER", name = "햄스터", species = "hamster"),
                PetSeed(key = "PENGUIN", name = "펭귄", species = "penguin"),
                PetSeed(key = "DOG", name = "강아지", species = "dog"),
                PetSeed(key = "MOLE", name = "두더지", species = "mole"),
            )
    }
}
