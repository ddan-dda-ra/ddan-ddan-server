package notbe.tmtm.ddanddanserver.infrastructure.database.seed

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import notbe.tmtm.ddanddanserver.infrastructure.database.entity.PetCatalogEntity
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.PetCatalogRepository

class PetCatalogSeederTest : FunSpec({
    lateinit var repository: PetCatalogRepository
    lateinit var seeder: PetCatalogSeeder

    beforeEach {
        repository = mockk(relaxed = true)
        seeder = PetCatalogSeeder(repository)
    }

    test("seed는 빈 컬렉션에 5종 기본 펫을 등록한다") {
        every { repository.count() } returns 0L
        val captured = slot<List<PetCatalogEntity>>()
        every { repository.saveAll(capture(captured)) } answers { captured.captured }

        seeder.seed()

        val saved = captured.captured
        saved.map { it.type } shouldContainExactlyInAnyOrder listOf("CAT", "HAMSTER", "PENGUIN", "DOG", "MOLE")
    }

    test("seed의 5종 colorCode는 설계 표 5색과 일치한다") {
        every { repository.count() } returns 0L
        val captured = slot<List<PetCatalogEntity>>()
        every { repository.saveAll(capture(captured)) } answers { captured.captured }

        seeder.seed()

        val colorByType = captured.captured.associate { it.type to it.colorCode }
        colorByType["CAT"] shouldBe "#FD85FF"
        colorByType["HAMSTER"] shouldBe "#46F8A2"
        colorByType["PENGUIN"] shouldBe "#4E95FF"
        colorByType["DOG"] shouldBe "#9B6CFF"
        colorByType["MOLE"] shouldBe "#D0DAE4"
    }

    test("seed는 displayOrder를 0부터 순차 부여한다 (CAT=0, HAMSTER=1, PENGUIN=2, DOG=3, MOLE=4)") {
        every { repository.count() } returns 0L
        val captured = slot<List<PetCatalogEntity>>()
        every { repository.saveAll(capture(captured)) } answers { captured.captured }

        seeder.seed()

        val orderByType = captured.captured.associate { it.type to it.displayOrder }
        orderByType["CAT"] shouldBe 0
        orderByType["HAMSTER"] shouldBe 1
        orderByType["PENGUIN"] shouldBe 2
        orderByType["DOG"] shouldBe 3
        orderByType["MOLE"] shouldBe 4
    }

    test("seed는 이미 데이터가 존재하면 saveAll을 호출하지 않는다") {
        every { repository.count() } returns 5L

        seeder.seed()

        verify(exactly = 0) { repository.saveAll(any<List<PetCatalogEntity>>()) }
    }

    test("seed는 5종 모두 isActive=true이며 level=1..5의 lottie URL을 채운다") {
        every { repository.count() } returns 0L
        val captured = slot<List<PetCatalogEntity>>()
        every { repository.saveAll(capture(captured)) } answers { captured.captured }

        seeder.seed()

        captured.captured.forEach { entity ->
            entity.isActive shouldBe true
            entity.levels.keys shouldContainExactlyInAnyOrder (1..5).toList()
        }
    }
})
