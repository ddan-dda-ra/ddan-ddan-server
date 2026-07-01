package notbe.tmtm.ddanddanserver.application.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import notbe.tmtm.ddanddanserver.domain.exception.PetCatalogInactiveException
import notbe.tmtm.ddanddanserver.domain.exception.PetCatalogNotFoundException
import notbe.tmtm.ddanddanserver.infrastructure.database.entity.PetCatalogEntity
import notbe.tmtm.ddanddanserver.infrastructure.database.entity.PetCatalogLevelEntity
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.PetCatalogRepository
import java.time.Instant

class PetCatalogServiceTest : FunSpec({
    lateinit var repository: PetCatalogRepository
    lateinit var service: PetCatalogService

    fun entity(
        type: String,
        active: Boolean = true,
        order: Int = 1,
        updatedAt: Instant = Instant.parse("2026-07-01T00:00:00Z"),
    ) = PetCatalogEntity(
        type = type,
        name = type,
        colorCode = "#AABBCC",
        isActive = active,
        displayOrder = order,
        levels = (1..5).associateWith {
            PetCatalogLevelEntity(
                "https://cdn.test/$it.svg",
                "https://cdn.test/$it.json",
                "https://cdn.test/${it}_play.json",
            )
        },
        createdAt = updatedAt,
        updatedAt = updatedAt,
    )

    beforeEach {
        repository = mockk()
        service = PetCatalogService(repository)
    }

    test("공개 카탈로그는 활성 항목만 반환하고 revision은 updatedAt 최댓값이다") {
        val older = Instant.parse("2026-07-01T00:00:00Z")
        val newer = Instant.parse("2026-07-02T03:04:05Z")
        every { repository.findAllByIsActiveTrueOrderByDisplayOrderAscTypeAsc() } returns
            listOf(entity("CAT", updatedAt = older), entity("DOG", order = 2, updatedAt = newer))

        val result = service.getCatalog()

        result.pets.map { it.type } shouldContainExactly listOf("CAT", "DOG")
        result.revision shouldBe newer
    }

    test("활성 카탈로그가 비어 있으면 revision은 epoch이다") {
        every { repository.findAllByIsActiveTrueOrderByDisplayOrderAscTypeAsc() } returns emptyList()

        service.getCatalog().revision shouldBe Instant.EPOCH
    }

    test("활성 여부 확인은 type을 정규화하고 비활성 항목이면 예외가 발생한다") {
        every { repository.findByType("CAT") } returns entity("CAT", active = false)

        shouldThrow<PetCatalogInactiveException> { service.requireActive(" cat ") }
        verify { repository.findByType("CAT") }
    }

    test("활성 여부 확인 시 카탈로그 항목이 없으면 예외가 발생한다") {
        every { repository.findByType("CAT") } returns null

        shouldThrow<PetCatalogNotFoundException> { service.requireActive("cat") }
    }

    test("랜덤 선택은 제외 type을 정규화하고 활성 목록만 사용한다") {
        every { repository.findAllByIsActiveTrueOrderByDisplayOrderAscTypeAsc() } returns
            listOf(entity("CAT"), entity("DOG"))

        service.pickRandomActiveExcluding(listOf(" cat ")) shouldBe "DOG"
    }
})
