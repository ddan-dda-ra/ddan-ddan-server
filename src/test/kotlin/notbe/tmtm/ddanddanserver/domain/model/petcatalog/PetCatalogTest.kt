package notbe.tmtm.ddanddanserver.domain.model.petcatalog

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import notbe.tmtm.ddanddanserver.domain.exception.PetCatalogInvalidException

class PetCatalogTest : FunSpec({
    fun level(number: Int, image: String = "https://cdn.test/$number.svg") =
        PetCatalogLevel(number, image, "https://cdn.test/$number.json", "https://cdn.test/${number}_play.json")
    fun create(levels: List<PetCatalogLevel> = (1..5).map(::level)) =
        PetCatalogItem.create(" cat_1 ", "  고양이  ", " #a1b2c3 ", false, 0, levels)

    test("type 이름 색상을 canonicalize하고 역순 레벨을 오름차순으로 정렬한다") {
        val item = create((1..5).reversed().map(::level))
        item.type shouldBe "CAT_1"
        item.name shouldBe "고양이"
        item.colorCode shouldBe "#A1B2C3"
        item.levels.map { it.level } shouldContainExactly (1..5).toList()
    }

    test("레벨 누락 중복 범위 밖 입력은 모두 PC004로 거부한다") {
        listOf((1..4).map(::level), listOf(1, 2, 3, 4, 4).map(::level), listOf(0, 1, 2, 3, 4).map(::level), listOf(1, 2, 3, 4, 6).map(::level)).forEach {
            shouldThrow<PetCatalogInvalidException> { create(it) }
        }
    }

    test("잘못된 type 이름 색상과 음수 displayOrder는 PC004로 거부한다") {
        val valid = (1..5).map(::level)
        shouldThrow<PetCatalogInvalidException> { PetCatalogItem.create("1CAT", "고양이", "#AABBCC", false, 0, valid) }
        shouldThrow<PetCatalogInvalidException> { PetCatalogItem.create("CAT", "   ", "#AABBCC", false, 0, valid) }
        shouldThrow<PetCatalogInvalidException> { PetCatalogItem.create("CAT", "고양이", "red", false, 0, valid) }
        shouldThrow<PetCatalogInvalidException> { PetCatalogItem.create("CAT", "고양이", "#AABBCC", false, -1, valid) }
    }

    test("이미지는 SVG PNG WebP를 대소문자 무관하게 허용하고 Lottie는 JSON만 허용한다") {
        listOf("svg", "PNG", "WebP").forEach { PetCatalogAssetUrl.image("https://cdn.test/a.$it?rev=1") }
        PetCatalogAssetUrl.lottie("https://cdn.test/a.JSON?rev=1")
        shouldThrow<PetCatalogInvalidException> { PetCatalogAssetUrl.image("https://cdn.test/a.gif") }
        shouldThrow<PetCatalogInvalidException> { PetCatalogAssetUrl.lottie("https://cdn.test/a.svg") }
    }

    test("에셋 URL은 absolute host를 요구하고 user-info fragment 공백을 거부한다") {
        listOf("/a.svg", "https:///a.svg", "https://user@cdn.test/a.svg", "https://cdn.test/a.svg#x", "https://cdn.test/a file.svg").forEach {
            shouldThrow<PetCatalogInvalidException> { PetCatalogAssetUrl.image(it) }
        }
    }
})
