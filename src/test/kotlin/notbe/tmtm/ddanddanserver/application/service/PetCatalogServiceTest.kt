package notbe.tmtm.ddanddanserver.application.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import notbe.tmtm.ddanddanserver.domain.exception.PetCatalogDuplicateKeyException
import notbe.tmtm.ddanddanserver.domain.exception.PetCatalogInactiveException
import notbe.tmtm.ddanddanserver.domain.exception.PetCatalogInvalidException
import notbe.tmtm.ddanddanserver.domain.exception.PetCatalogNotFoundException
import notbe.tmtm.ddanddanserver.domain.model.petcatalog.PetCatalogAssetUrl
import notbe.tmtm.ddanddanserver.domain.model.petcatalog.PetCatalogLevel
import notbe.tmtm.ddanddanserver.infrastructure.database.entity.PetCatalogEntity
import notbe.tmtm.ddanddanserver.infrastructure.database.entity.PetCatalogLevelEntity
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.PetCatalogRepository
import org.springframework.dao.DuplicateKeyException
import java.time.Instant

class PetCatalogServiceTest : FunSpec({
    lateinit var repository: PetCatalogRepository
    lateinit var policy: PetCatalogAssetUrlPolicy
    lateinit var service: PetCatalogService

    fun levels() = (1..5).map { PetCatalogLevel(it, "https://cdn.test/$it.svg", "https://cdn.test/$it.json", "https://cdn.test/${it}_play.json") }
    fun command(type: String = "CAT", active: Boolean = false, levels: List<PetCatalogLevel> = levels()) =
        UpsertPetCatalogCommand(type, " 고양이 ", " #aabbcc ", active, 1, levels)
    fun entity(
        type: String,
        active: Boolean = true,
        order: Int = 1,
        updatedAt: Instant = Instant.parse("2026-07-01T00:00:00Z"),
    ) = PetCatalogEntity(
        type = type, name = type, colorCode = "#AABBCC", isActive = active, displayOrder = order,
        levels = (1..5).associateWith { PetCatalogLevelEntity("https://cdn.test/$it.svg", "https://cdn.test/$it.json", "https://cdn.test/${it}_play.json") },
        createdAt = updatedAt,
        updatedAt = updatedAt,
    )

    beforeEach {
        repository = mockk()
        policy = mockk()
        every { policy.validate(any<PetCatalogAssetUrl>()) } returns Unit
        service = PetCatalogService(repository, policy)
    }

    test("공개 카탈로그는 활성 항목만 반환하고 revision은 active updatedAt 최댓값이다") {
        val older = Instant.parse("2026-07-01T00:00:00Z")
        val newer = Instant.parse("2026-07-02T03:04:05Z")
        every { repository.findAllByIsActiveTrueOrderByDisplayOrderAscTypeAsc() } returns
            listOf(entity("CAT", updatedAt = older), entity("DOG", order = 2, updatedAt = newer))

        val result = service.getCatalog()

        result.pets.map { it.type } shouldContainExactly listOf("CAT", "DOG")
        result.revision shouldBe newer
    }

    test("활성 카탈로그가 비어 있으면 revision은 Instant EPOCH이다") {
        every { repository.findAllByIsActiveTrueOrderByDisplayOrderAscTypeAsc() } returns emptyList()

        service.getCatalog().revision shouldBe Instant.EPOCH
    }

    test("비활성 항목의 더 최신 updatedAt은 public revision에 영향을 주지 않는다") {
        val activeUpdatedAt = Instant.parse("2026-07-01T00:00:00Z")
        val inactiveUpdatedAt = Instant.parse("2026-07-03T00:00:00Z")
        every { repository.findAllByIsActiveTrueOrderByDisplayOrderAscTypeAsc() } returns
            listOf(entity("CAT", updatedAt = activeUpdatedAt))
        every { repository.findAllByOrderByDisplayOrderAscTypeAsc() } returns
            listOf(entity("CAT", updatedAt = activeUpdatedAt), entity("FOX", active = false, updatedAt = inactiveUpdatedAt))

        service.getAllForAdmin()
        service.getCatalog().revision shouldBe activeUpdatedAt
    }

    test("관리자 카탈로그는 비활성 항목을 포함한다") {
        every { repository.findAllByOrderByDisplayOrderAscTypeAsc() } returns listOf(entity("CAT"), entity("FOX", false, 2))
        service.getAllForAdmin().map { it.isActive } shouldContainExactly listOf(true, false)
    }

    test("생성은 type 이름 색상을 canonical 형태로 저장하고 레벨을 정렬한다") {
        val saved = slot<PetCatalogEntity>()
        every { repository.save(capture(saved)) } answers { saved.captured }
        val result = service.create(command(" cat ", levels = levels().reversed()))
        result.type shouldBe "CAT"
        result.name shouldBe "고양이"
        result.colorCode shouldBe "#AABBCC"
        result.levels.map { it.level } shouldContainExactly (1..5).toList()
        saved.captured.levels.keys shouldBe (1..5).toSet()
    }

    test("생성 중 DB type 충돌은 PC002 예외로 변환한다") {
        every { repository.save(any()) } throws DuplicateKeyException("duplicate")
        shouldThrow<PetCatalogDuplicateKeyException> { service.create(command()) }
    }

    test("수정은 canonical path type으로 조회하고 비활성 항목을 활성화한다") {
        val before = Instant.parse("2020-01-01T00:00:00Z")
        val existing = entity("CAT", false, updatedAt = before)
        val saved = slot<PetCatalogEntity>()
        every { repository.findByType("CAT") } returns existing
        every { repository.save(capture(saved)) } answers { saved.captured }
        every { repository.findAllByIsActiveTrueOrderByDisplayOrderAscTypeAsc() } answers { listOf(saved.captured) }

        service.update(" cat ", command(active = true)).isActive shouldBe true

        service.getCatalog().revision shouldBe saved.captured.updatedAt
        (saved.captured.updatedAt > before) shouldBe true
        verify { repository.findByType("CAT") }
    }

    test("활성 항목을 수정하면 public revision이 변경된다") {
        val before = Instant.parse("2020-01-01T00:00:00Z")
        val existing = entity("CAT", true, updatedAt = before)
        val saved = slot<PetCatalogEntity>()
        every { repository.findByType("CAT") } returns existing
        every { repository.save(capture(saved)) } answers { saved.captured }
        every { repository.findAllByIsActiveTrueOrderByDisplayOrderAscTypeAsc() } returns listOf(existing) andThenAnswer { listOf(saved.captured) }

        val oldRevision = service.getCatalog().revision
        service.update("CAT", command(active = true))
        val newRevision = service.getCatalog().revision

        oldRevision shouldBe before
        newRevision shouldBe saved.captured.updatedAt
        (newRevision > oldRevision) shouldBe true
    }

    test("활성 항목을 비활성화하면 PC004 예외가 발생하고 저장하지 않는다") {
        every { repository.findByType("CAT") } returns entity("CAT", true)
        shouldThrow<PetCatalogInvalidException> { service.update("CAT", command(active = false)) }
        verify(exactly = 0) { repository.save(any()) }
    }

    test("path type과 command type이 다르면 PC004 예외가 발생한다") {
        every { repository.findByType("CAT") } returns entity("CAT", false)
        shouldThrow<PetCatalogInvalidException> { service.update("CAT", command("DOG")) }
    }

    test("수정 대상이 없으면 canonical type을 담은 PC001 예외가 발생한다") {
        every { repository.findByType("CAT") } returns null
        shouldThrow<PetCatalogNotFoundException> { service.update(" cat ", command()) }
    }

    test("활성 여부 확인은 canonical type을 조회하고 비활성이면 PC003 예외가 발생한다") {
        every { repository.findByType("CAT") } returns entity("CAT", false)
        shouldThrow<PetCatalogInactiveException> { service.requireActive(" cat ") }
    }

    test("랜덤 선택은 제외 type도 canonicalize하고 활성 목록만 사용한다") {
        every { repository.findAllByIsActiveTrueOrderByDisplayOrderAscTypeAsc() } returns listOf(entity("CAT"), entity("DOG"))
        service.pickRandomActiveExcluding(listOf(" cat ")) shouldBe "DOG"
    }
})
