package notbe.tmtm.ddanddanserver.domain.model.pet

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.bson.types.ObjectId

class PetTest : FunSpec({

    test("레벨 1: 경험치 0-499는 레벨 1이어야 한다") {
        val pet = Pet(
            id = ObjectId(),
            type = "DOG",
            ownerUserId = ObjectId(),
            exp = 0
        )

        pet.getLevel() shouldBe 1

        pet.exp = 250
        pet.getLevel() shouldBe 1

        pet.exp = 499
        pet.getLevel() shouldBe 1
    }

    test("레벨 2: 경험치 500-999는 레벨 2여야 한다") {
        val pet = Pet(
            id = ObjectId(),
            type = "DOG",
            ownerUserId = ObjectId(),
            exp = 500
        )

        pet.getLevel() shouldBe 2

        pet.exp = 750
        pet.getLevel() shouldBe 2

        pet.exp = 999
        pet.getLevel() shouldBe 2
    }

    test("레벨 3: 경험치 1000-1999는 레벨 3이어야 한다") {
        val pet = Pet(
            id = ObjectId(),
            type = "DOG",
            ownerUserId = ObjectId(),
            exp = 1000
        )

        pet.getLevel() shouldBe 3

        pet.exp = 1500
        pet.getLevel() shouldBe 3

        pet.exp = 1999
        pet.getLevel() shouldBe 3
    }

    test("레벨 4: 경험치 2000-3999는 레벨 4여야 한다") {
        val pet = Pet(
            id = ObjectId(),
            type = "DOG",
            ownerUserId = ObjectId(),
            exp = 2000
        )

        pet.getLevel() shouldBe 4

        pet.exp = 3000
        pet.getLevel() shouldBe 4

        pet.exp = 3999
        pet.getLevel() shouldBe 4
    }

    test("높은 레벨: 높은 경험치에서도 올바른 레벨을 반환해야 한다") {
        val pet = Pet(
            id = ObjectId(),
            type = "DOG",
            ownerUserId = ObjectId(),
            exp = 4000
        )

        pet.getLevel() shouldBe 5

        pet.exp = 8000
        pet.getLevel() shouldBe 6

        pet.exp = 16000
        pet.getLevel() shouldBe 7
    }

    test("경험치 퍼센트 계산: 레벨 1에서 올바르게 계산해야 한다") {
        val pet = Pet(
            id = ObjectId(),
            type = "DOG",
            ownerUserId = ObjectId(),
            exp = 250
        )

        val expPercent = pet.getExpPercent()

        expPercent shouldBe 50.0 // 250/500 * 100 = 50%
    }

    test("경험치 퍼센트 계산: 레벨 2에서 올바르게 계산해야 한다") {
        val pet = Pet(
            id = ObjectId(),
            type = "DOG",
            ownerUserId = ObjectId(),
            exp = 750
        )

        val expPercent = pet.getExpPercent()

        expPercent shouldBe 50.0 // (750-500)/500 * 100 = 50%
    }

    test("최대 레벨 확인: isMaxLevel은 항상 false를 반환해야 한다") {
        val pet = Pet(
            id = ObjectId(),
            type = "DOG",
            ownerUserId = ObjectId(),
            exp = 999999
        )

        pet.isMaxLevel() shouldBe false
    }

    test("레벨업 판정: 이전 경험치 기준 레벨과 현재 레벨을 비교해야 한다") {
        val pet = Pet(
            id = ObjectId(),
            type = "DOG",
            ownerUserId = ObjectId(),
            exp = 4000
        )

        val result = pet.levelUpResultSince(4000 - Pet.EXP_PER_FOOD)

        result.previousLevel shouldBe 4
        result.currentLevel shouldBe 5
        result.shouldReceiveTicket() shouldBe true
    }

    test("레벨업 판정: 같은 레벨 안에서 오르면 티켓 대상이 아니어야 한다") {
        val pet = Pet(
            id = ObjectId(),
            type = "DOG",
            ownerUserId = ObjectId(),
            exp = 500
        )

        val result = pet.levelUpResultSince(500 - Pet.EXP_PER_TOY)

        result.previousLevel shouldBe 1
        result.currentLevel shouldBe 2
        result.shouldReceiveTicket() shouldBe false
    }
})
