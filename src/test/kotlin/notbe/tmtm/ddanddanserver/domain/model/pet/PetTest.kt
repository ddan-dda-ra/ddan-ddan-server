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

    test("먹이주기: 먹이를 주면 경험치가 100 증가해야 한다") {
        val pet = Pet(
            id = ObjectId(),
            type = "DOG",
            ownerUserId = ObjectId(),
            exp = 0
        )

        pet.eat()

        pet.exp shouldBe 100
        pet.getLevel() shouldBe 1
    }

    test("놀아주기: 놀아주면 경험치가 500 증가해야 한다") {
        val pet = Pet(
            id = ObjectId(),
            type = "DOG",
            ownerUserId = ObjectId(),
            exp = 0
        )

        pet.play()

        pet.exp shouldBe 500
        pet.getLevel() shouldBe 2
    }
})
