package notbe.tmtm.ddanddanserver.application.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import notbe.tmtm.ddanddanserver.domain.exception.PetCatalogDuplicateKeyException
import notbe.tmtm.ddanddanserver.domain.exception.PetCatalogNotFoundException
import notbe.tmtm.ddanddanserver.domain.model.petcatalog.PetCatalogBackgrounds
import notbe.tmtm.ddanddanserver.domain.model.petcatalog.PetCatalogLevel
import notbe.tmtm.ddanddanserver.infrastructure.database.entity.PetCatalogBackgroundsEntity
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
            type = key,
            name = key,
            backgrounds =
                PetCatalogBackgroundsEntity(
                    homeUrl = "https://cdn/$key/backgrounds/home.png",
                    homeCompactUrl = "https://cdn/$key/backgrounds/home_compact.png",
                    friendCardUrl = "https://cdn/$key/backgrounds/friend_card.png",
                ),
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

        result.pets.map { it.type } shouldBe listOf("CAT", "DOG")
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

    // --- Admin operations ---

    test("getAllForAdmin은 비활성 펫도 포함하여 반환한다") {
        every { repository.findAllByOrderByDisplayOrderAsc() } returns
            listOf(
                entity("CAT", order = 0, isActive = true),
                entity("HIDDEN", order = 1, isActive = false),
            )

        val result = service.getAllForAdmin()

        result.map { it.type } shouldBe listOf("CAT", "HIDDEN")
        result[1].isActive shouldBe false
    }

    fun sampleLevels(): Map<Int, PetCatalogLevel> =
        mapOf(
            1 to PetCatalogLevel(
                imageUrl = "https://cdn/x/1.png",
                lottieDefaultUrl = "https://cdn/x/1_default.json",
                lottiePlayEatUrl = "https://cdn/x/1_play_eat.json",
            ),
        )

    fun sampleBackgrounds(): PetCatalogBackgrounds =
        PetCatalogBackgrounds(
            homeUrl = "https://cdn/x/backgrounds/home.png",
            homeCompactUrl = "https://cdn/x/backgrounds/home_compact.png",
            friendCardUrl = "https://cdn/x/backgrounds/friend_card.png",
        )

    test("create는 신규 펫을 저장하고 versionCache를 invalidate한다") {
        // Given — cache를 미리 채움
        val initialVersion = Instant.parse("2026-05-01T00:00:00Z")
        every { repository.findAll() } returns listOf(entity("CAT", order = 0, updatedAt = initialVersion))
        service.currentVersion() shouldBe initialVersion

        // When — create
        val saved = slot<PetCatalogEntity>()
        every { repository.save(capture(saved)) } answers { saved.captured }
        val newVersion = Instant.parse("2026-05-04T12:00:00Z")
        every { repository.findAll() } answers {
            listOf(
                entity("CAT", order = 0, updatedAt = initialVersion),
                entity("QUOKKA", order = 5, updatedAt = newVersion),
            )
        }

        val result = service.create("QUOKKA", "쿼카", sampleBackgrounds(), true, 5, sampleLevels())

        // Then — save + cache invalidate(다음 currentVersion이 새 값으로 조회)
        result.type shouldBe "QUOKKA"
        result.name shouldBe "쿼카"
        saved.captured.type shouldBe "QUOKKA"
        service.currentVersion() shouldBe newVersion
    }

    test("create 시 DB unique index 충돌(DuplicateKeyException)을 PetCatalogDuplicateKeyException으로 변환한다") {
        every {
            repository.save(any())
        } throws org.springframework.dao.DuplicateKeyException("E11000 duplicate key error")

        shouldThrow<PetCatalogDuplicateKeyException> {
            service.create("CAT", "고양이2", sampleBackgrounds(), true, 0, sampleLevels())
        }
    }

    test("update는 기존 펫의 필드를 갱신한다") {
        val existing = entity("CAT", order = 0, isActive = true)
        every { repository.findByType("CAT") } returns existing
        val saved = slot<PetCatalogEntity>()
        every { repository.save(capture(saved)) } answers { saved.captured }
        every { repository.findAll() } answers { listOf(saved.captured) }

        val result =
            service.update(
                type = "CAT",
                name = "갱신된고양이",
                backgrounds = sampleBackgrounds(),
                isActive = false,
                displayOrder = 99,
                levels = sampleLevels(),
            )

        result.name shouldBe "갱신된고양이"
        result.isActive shouldBe false
        result.displayOrder shouldBe 99
    }

    test("update 시 존재하지 않는 키이면 PetCatalogNotFoundException") {
        every { repository.findByType("UNKNOWN") } returns null

        shouldThrow<PetCatalogNotFoundException> {
            service.update("UNKNOWN", "x", sampleBackgrounds(), true, 0, sampleLevels())
        }
    }

    test("softDelete는 isActive=false로 갱신하고 동일 펫을 반환한다") {
        val existing = entity("CAT", order = 0, isActive = true)
        every { repository.findByType("CAT") } returns existing
        val saved = slot<PetCatalogEntity>()
        every { repository.save(capture(saved)) } answers { saved.captured }

        val result = service.softDelete("CAT")

        result.isActive shouldBe false
        saved.captured.isActive shouldBe false
    }

    test("softDelete 시 이미 비활성인 펫은 save 호출 없이 그대로 반환한다") {
        val existing = entity("HIDDEN", order = 0, isActive = false)
        every { repository.findByType("HIDDEN") } returns existing

        val result = service.softDelete("HIDDEN")

        result.isActive shouldBe false
        verify(exactly = 0) { repository.save(any()) }
    }

    test("create/update/softDelete 후 versionCache가 invalidate되어 currentVersion이 다시 조회한다") {
        // 첫 currentVersion으로 캐시 채움
        val initialVersion = Instant.parse("2026-05-01T00:00:00Z")
        every { repository.findAll() } returns listOf(entity("CAT", order = 0, updatedAt = initialVersion))
        service.currentVersion() shouldBe initialVersion

        // softDelete 동작
        every { repository.findByType("CAT") } returns entity("CAT", order = 0, isActive = true, updatedAt = initialVersion)
        val newVersion = Instant.parse("2026-05-04T12:00:00Z")
        every { repository.save(any()) } returns entity("CAT", order = 0, isActive = false, updatedAt = newVersion)
        every { repository.findAll() } returns listOf(entity("CAT", order = 0, isActive = false, updatedAt = newVersion))

        service.softDelete("CAT")

        // 캐시가 invalidate되어 새 version 반환
        service.currentVersion() shouldBe newVersion
    }
})
