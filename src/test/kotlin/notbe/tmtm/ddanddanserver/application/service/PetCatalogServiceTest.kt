package notbe.tmtm.ddanddanserver.application.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import notbe.tmtm.ddanddanserver.infrastructure.database.entity.PetCatalogEntity
import notbe.tmtm.ddanddanserver.infrastructure.database.entity.PetCatalogLevelEntity
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.PetCatalogRepository
import java.time.Instant

class PetCatalogServiceTest : FunSpec({
    lateinit var repository: PetCatalogRepository
    lateinit var service: PetCatalogService

    fun entity(
        key: String,
        order: Int,
        isActive: Boolean = true,
        updatedAt: Instant = Instant.parse("2026-05-04T00:00:00Z"),
    ): PetCatalogEntity =
        PetCatalogEntity(
            key = key,
            name = key,
            isActive = isActive,
            displayOrder = order,
            levels =
                (1..5).associateWith {
                    PetCatalogLevelEntity(
                        imageUrl = "https://cdn/$key/$it.png",
                        lottieDefaultUrl = "https://cdn/$key/${it}_default.json",
                        lottiePlayEatUrl = "https://cdn/$key/${it}_play_eat.json",
                    )
                },
            createdAt = updatedAt,
            updatedAt = updatedAt,
        )

    beforeEach {
        repository = mockk()
        service = PetCatalogService(repository)
    }

    test("getActiveCatalog는 displayOrder 순서대로 활성 펫만 반환한다") {
        val entities =
            listOf(
                entity("CAT", order = 0),
                entity("DOG", order = 1),
            )
        every { repository.findAllByIsActiveTrueOrderByDisplayOrderAsc() } returns entities
        every { repository.findAll() } returns entities

        val result = service.getActiveCatalog()

        result.pets.map { it.key } shouldBe listOf("CAT", "DOG")
    }

    test("getActiveCatalog의 version은 currentVersion과 동일하다 (헤더와 응답 body 버전 일치 보장)") {
        val activeOldUpdatedAt = Instant.parse("2026-05-01T00:00:00Z")
        val inactiveNewUpdatedAt = Instant.parse("2026-05-04T12:00:00Z")
        every { repository.findAllByIsActiveTrueOrderByDisplayOrderAsc() } returns
            listOf(entity("CAT", order = 0, updatedAt = activeOldUpdatedAt))
        every { repository.findAll() } returns
            listOf(
                entity("CAT", order = 0, updatedAt = activeOldUpdatedAt),
                entity("HIDDEN", order = 99, isActive = false, updatedAt = inactiveNewUpdatedAt),
            )

        val catalog = service.getActiveCatalog()

        // 비활성 펫이 가장 최신 updatedAt이라도 헤더(currentVersion)와 응답 body version이 일치
        catalog.version shouldBe service.currentVersion()
        catalog.version shouldBe inactiveNewUpdatedAt
    }

    test("데이터가 비어있으면 catalog version은 Instant EPOCH이다") {
        every { repository.findAllByIsActiveTrueOrderByDisplayOrderAsc() } returns emptyList()
        every { repository.findAll() } returns emptyList()

        service.getActiveCatalog().version shouldBe Instant.EPOCH
    }

    test("currentVersion은 max(updatedAt)을 반환한다") {
        val newest = Instant.parse("2026-05-04T18:00:00Z")
        every { repository.findAll() } returns
            listOf(
                entity("CAT", order = 0, updatedAt = Instant.parse("2026-05-01T00:00:00Z")),
                entity("DOG", order = 1, updatedAt = newest),
            )

        service.currentVersion() shouldBe newest
    }

    test("currentVersion은 60초 TTL 캐시로 동일 시점 반복 호출 시 repository를 한 번만 조회한다") {
        every { repository.findAll() } returns listOf(entity("CAT", order = 0))

        repeat(5) { service.currentVersion() }

        verify(exactly = 1) { repository.findAll() }
    }

    test("데이터가 없으면 currentVersion은 Instant EPOCH를 반환한다") {
        every { repository.findAll() } returns emptyList()

        service.currentVersion() shouldBe Instant.EPOCH
    }
})
