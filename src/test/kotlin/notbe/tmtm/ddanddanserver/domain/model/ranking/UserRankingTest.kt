package notbe.tmtm.ddanddanserver.domain.model.ranking

import notbe.tmtm.ddanddanserver.domain.model.pet.Pet
import notbe.tmtm.ddanddanserver.domain.model.user.User
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class UserRankingTest {
    private val user = createUser("user")
    private val pet = createPet(user.id)

    @Test
    fun `UserRanking을 올바른 속성으로 생성해야 한다`() {
        // given
        val userStat = createUserStat(user, pet, 1000, 10)
        val rank = 5

        // when
        val userRanking = UserRanking(userStat, rank)

        // then
        assertEquals(userStat, userRanking.userStat)
        assertEquals(rank, userRanking.rank)
        assertEquals(user, userRanking.userStat.user)
        assertEquals("user", userRanking.userStat.user.name)
        assertEquals(1000, userRanking.userStat.totalCalories)
        assertEquals(10, userRanking.userStat.totalSucceededDays)
    }

    @Test
    fun `데이터 클래스 동등성을 지원해야 한다`() {
        // given
        val userStat = createUserStat(user, pet, 1000, 10)
        val rank = 3

        val userRanking1 = UserRanking(userStat, rank)
        val userRanking2 = UserRanking(userStat, rank)
        val userRanking3 = UserRanking(userStat, 4) // 다른 순위

        // then
        assertEquals(userRanking1, userRanking2)
        assertNotEquals(userRanking1, userRanking3)
        assertEquals(userRanking1.hashCode(), userRanking2.hashCode())
    }

    @Test
    fun `데이터 클래스 복사를 지원해야 한다`() {
        // given
        val userStat = createUserStat(user, pet, 1000, 10)
        val originalRanking = UserRanking(userStat, 3)

        // when
        val copiedRanking = originalRanking.copy(rank = 5)

        // then
        assertEquals(originalRanking.userStat, copiedRanking.userStat)
        assertEquals(3, originalRanking.rank)
        assertEquals(5, copiedRanking.rank)
    }

    @Test
    fun `데이터 클래스 toString을 지원해야 한다`() {
        // given
        val userStat = createUserStat(user, pet, 1000, 10)
        val userRanking = UserRanking(userStat, 1)

        // when
        val toString = userRanking.toString()

        // then
        assert(toString.contains("UserRanking"))
        assert(toString.contains("rank=1"))
        assert(toString.contains("userStat="))
    }

    @Test
    fun `다양한 순위 값을 올바르게 처리해야 한다`() {
        // given
        val userStat = createUserStat(user, pet, 1000, 10)

        // when
        val firstPlace = UserRanking(userStat, 1)
        val lastPlace = UserRanking(userStat, 100)
        val tiedRanking = UserRanking(userStat, 2)

        // then
        assertEquals(1, firstPlace.rank)
        assertEquals(100, lastPlace.rank)
        assertEquals(2, tiedRanking.rank)
        assertEquals(userStat, firstPlace.userStat)
        assertEquals(userStat, lastPlace.userStat)
        assertEquals(userStat, tiedRanking.userStat)
    }

    @Test
    fun `다양한 펫 타입과 함께 작동해야 한다`() {
        // given
        val dogUser = createUser("강아지주인")
        val dogPet = Pet.register("DOG", dogUser.id)
        val catUser = createUser("고양이주인")
        val catPet = Pet.register("CAT", catUser.id)

        // when
        val dogRanking =
            UserRanking(
                createUserStat(dogUser, dogPet, 1000, 10),
                1,
            )
        val catRanking =
            UserRanking(
                createUserStat(catUser, catPet, 800, 8),
                2,
            )

        // then
        assertEquals(dogPet.type, dogRanking.userStat.mainPet.type)
        assertEquals(catPet.type, catRanking.userStat.mainPet.type)
        assertEquals(1, dogRanking.rank)
        assertEquals(2, catRanking.rank)
    }

    private fun createUser(name: String): User = User.register("deviceToken-${name.lowercase()}", name)

    private fun createPet(ownerId: ObjectId): Pet = Pet.register(listOf("CAT", "DOG", "HAMSTER", "PENGUIN", "MOLE").random(), ownerId)

    private fun createUserStat(
        user: User,
        mainPet: Pet,
        totalCalories: Int,
        totalSucceededDays: Int,
    ): UserStat {
        return UserStat(
            user = user,
            mainPet = mainPet,
            totalCalories = totalCalories,
            totalSucceededDays = totalSucceededDays,
        )
    }
}
