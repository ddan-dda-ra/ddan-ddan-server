package notbe.tmtm.ddanddanserver.domain.model.user

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import notbe.tmtm.ddanddanserver.domain.model.cheer.Cheer
import notbe.tmtm.ddanddanserver.domain.model.pet.Pet
import notbe.tmtm.ddanddanserver.domain.model.pet.PetType
import org.bson.types.ObjectId
import java.time.LocalDate

class UserProfileTest : FunSpec({
    test("오늘 응원받았는지 확인할 수 있다") {
        val userId = ObjectId()
        val cheererId = ObjectId()
        val today = LocalDate.now()

        val user = User(
            id = userId,
            deviceToken = DeviceToken("test-token"),
            name = "테스트유저",
            setting = UserSetting(isAppPushOn = true)
        )

        val mainPet = Pet(
            id = ObjectId(),
            type = PetType.CAT,
            ownerUserId = userId,
            exp = 100
        )

        val todayDailyInfo = DailyInfo.create(
            userId = userId,
            userName = "테스트유저",
            petType = PetType.CAT,
            date = today
        )

        val todayCheer = Cheer.create(cheererId, userId, today)

        val userProfile = UserProfile(
            user = user,
            mainPet = mainPet,
            todayDailyInfo = todayDailyInfo,
            receivedCheers = listOf(todayCheer),
            isFriend = true
        )

        userProfile.isCheeredToday(cheererId) shouldBe true
    }

    test("오늘 응원받지 않았으면 false를 반환한다") {
        val userId = ObjectId()
        val cheererId = ObjectId()
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        val user = User(
            id = userId,
            deviceToken = DeviceToken("test-token"),
            name = "테스트유저",
            setting = UserSetting(isAppPushOn = true)
        )

        val mainPet = Pet(
            id = ObjectId(),
            type = PetType.CAT,
            ownerUserId = userId,
            exp = 100
        )

        val todayDailyInfo = DailyInfo.create(
            userId = userId,
            userName = "테스트유저",
            petType = PetType.CAT,
            date = today
        )

        val yesterdayCheer = Cheer.create(cheererId, userId, yesterday)

        val userProfile = UserProfile(
            user = user,
            mainPet = mainPet,
            todayDailyInfo = todayDailyInfo,
            receivedCheers = listOf(yesterdayCheer),
            isFriend = true
        )

        userProfile.isCheeredToday(cheererId) shouldBe false
    }

    test("다른 사용자로부터의 응원이면 false를 반환한다") {
        val userId = ObjectId()
        val cheererId = ObjectId()
        val otherCheererId = ObjectId()
        val today = LocalDate.now()

        val user = User(
            id = userId,
            deviceToken = DeviceToken("test-token"),
            name = "테스트유저",
            setting = UserSetting(isAppPushOn = true)
        )

        val mainPet = Pet(
            id = ObjectId(),
            type = PetType.CAT,
            ownerUserId = userId,
            exp = 100
        )

        val todayDailyInfo = DailyInfo.create(
            userId = userId,
            userName = "테스트유저",
            petType = PetType.CAT,
            date = today
        )

        val otherCheer = Cheer.create(otherCheererId, userId, today)

        val userProfile = UserProfile(
            user = user,
            mainPet = mainPet,
            todayDailyInfo = todayDailyInfo,
            receivedCheers = listOf(otherCheer),
            isFriend = true
        )

        userProfile.isCheeredToday(cheererId) shouldBe false
    }

    test("본인이 아닌 사용자는 친구로 간주한다") {
        val userId = ObjectId()
        val otherUserId = ObjectId()

        val user = User(
            id = userId,
            deviceToken = DeviceToken("test-token"),
            name = "테스트유저",
            setting = UserSetting(isAppPushOn = true)
        )

        val mainPet = Pet(
            id = ObjectId(),
            type = PetType.CAT,
            ownerUserId = userId,
            exp = 100
        )

        val todayDailyInfo = DailyInfo.create(
            userId = userId,
            userName = "테스트유저",
            petType = PetType.CAT
        )

        val userProfile = UserProfile(
            user = user,
            mainPet = mainPet,
            todayDailyInfo = todayDailyInfo,
            receivedCheers = emptyList(),
            isFriend = true
        )

        userProfile.isFriend shouldBe true
    }

    test("본인은 친구로 간주하지 않는다") {
        val userId = ObjectId()

        val user = User(
            id = userId,
            deviceToken = DeviceToken("test-token"),
            name = "테스트유저",
            setting = UserSetting(isAppPushOn = true)
        )

        val mainPet = Pet(
            id = ObjectId(),
            type = PetType.CAT,
            ownerUserId = userId,
            exp = 100
        )

        val todayDailyInfo = DailyInfo.create(
            userId = userId,
            userName = "테스트유저",
            petType = PetType.CAT
        )

        val userProfile = UserProfile(
            user = user,
            mainPet = mainPet,
            todayDailyInfo = todayDailyInfo,
            receivedCheers = emptyList(),
            isFriend = false
        )

        userProfile.isFriend shouldBe false
    }
})
