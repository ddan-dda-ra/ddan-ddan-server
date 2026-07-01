package notbe.tmtm.ddanddanserver.infrastructure.database.entity

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class PetCatalogEntityTest : FunSpec({
    test("DB levels Map을 정렬된 domain List로 변환하고 다시 Map으로 보존한다") {
        val entity = PetCatalogEntity(
            type = "CAT", name = "고양이", colorCode = "#AABBCC", isActive = true, displayOrder = 0,
            levels = (1..5).reversed().associateWith { PetCatalogLevelEntity("https://cdn.test/$it.svg", "https://cdn.test/$it.json", "https://cdn.test/${it}_play.json") },
        )
        val domain = entity.toDomain()
        domain.levels.map { it.level } shouldContainExactly (1..5).toList()
        PetCatalogEntity.fromDomain(domain).levels.keys shouldBe (1..5).toSet()
    }
})
