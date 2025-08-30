package notbe.tmtm.ddanddanserver.domain.model.cheer

import notbe.tmtm.ddanddanserver.domain.exception.CheerSelfException
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class CheerTest {
    @Test
    fun `응원을 생성할 수 있다`() {
        val cheererId = ObjectId()
        val cheereeId = ObjectId()
        val date = LocalDate.of(2024, 8, 23)

        val cheer = Cheer.create(cheererId, cheereeId, date)

        assertThat(cheer.cheererId).isEqualTo(cheererId)
        assertThat(cheer.cheereeId).isEqualTo(cheereeId)
        assertThat(cheer.date).isEqualTo(date)
        assertThat(cheer.createdAt).isNotNull()
        assertThat(cheer.id).isNotNull()
    }

    @Test
    fun `자기 자신을 응원할 수 없다`() {
        val userId = ObjectId()

        assertThrows<CheerSelfException> {
            Cheer.create(userId, userId)
        }
    }

    @Test
    fun `특정 사용자에 대한 응원인지 확인할 수 있다`() {
        val cheererId = ObjectId()
        val cheereeId = ObjectId()
        val otherUserId = ObjectId()

        val cheer = Cheer.create(cheererId, cheereeId)

        assertThat(cheer.isCheerFor(cheereeId)).isTrue()
        assertThat(cheer.isCheerFor(otherUserId)).isFalse()
    }

    @Test
    fun `특정 사용자가 한 응원인지 확인할 수 있다`() {
        val cheererId = ObjectId()
        val cheereeId = ObjectId()
        val otherUserId = ObjectId()

        val cheer = Cheer.create(cheererId, cheereeId)

        assertThat(cheer.isCheerBy(cheererId)).isTrue()
        assertThat(cheer.isCheerBy(otherUserId)).isFalse()
    }

    @Test
    fun `두 사용자 간의 응원인지 확인할 수 있다`() {
        val user1 = ObjectId()
        val user2 = ObjectId()
        val user3 = ObjectId()

        val cheer = Cheer.create(user1, user2)

        assertThat(cheer.isCheerBetween(user1, user2)).isTrue()
        assertThat(cheer.isCheerBetween(user2, user1)).isTrue()
        assertThat(cheer.isCheerBetween(user1, user3)).isFalse()
    }

    @Test
    fun `날짜를 지정하지 않으면 오늘 날짜로 생성된다`() {
        val cheererId = ObjectId()
        val cheereeId = ObjectId()

        val cheer = Cheer.create(cheererId, cheereeId)

        assertThat(cheer.date).isEqualTo(LocalDate.now())
    }
}
