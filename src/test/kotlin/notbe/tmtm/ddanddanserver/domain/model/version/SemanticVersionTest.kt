package notbe.tmtm.ddanddanserver.domain.model.version

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SemanticVersionTest {

    @Test
    fun `유효한 시맨틱 버전 파싱이 성공해야 한다`() {
        // given & when
        val version = SemanticVersion.parse("1.2.3")

        // then
        assertEquals(1, version.major)
        assertEquals(2, version.minor)
        assertEquals(3, version.patch)
    }

    @Test
    fun `0으로 시작하는 버전도 유효해야 한다`() {
        // given & when
        val version = SemanticVersion.parse("0.0.1")

        // then
        assertEquals(0, version.major)
        assertEquals(0, version.minor)
        assertEquals(1, version.patch)
    }

    @Test
    fun `큰 버전 번호도 파싱이 가능해야 한다`() {
        // given & when
        val version = SemanticVersion.parse("123.456.789")

        // then
        assertEquals(123, version.major)
        assertEquals(456, version.minor)
        assertEquals(789, version.patch)
    }

    @Test
    fun `잘못된 형식의 버전은 예외를 발생시켜야 한다`() {
        // given
        val invalidVersions = listOf(
            "1.2",        // patch 누락
            "1.2.3.4",    // 추가 부분
            "1.2.a",      // 문자 포함
            "a.b.c",      // 모두 문자
            "1..3",       // 빈 minor
            ".2.3",       // 빈 major
            "1.2.",       // 빈 patch
            "",           // 빈 문자열
            "v1.2.3",     // 접두사 포함
            "1.2.3-alpha" // 접미사 포함
        )

        // when & then
        invalidVersions.forEach { invalidVersion ->
            assertThrows<IllegalArgumentException> {
                SemanticVersion.parse(invalidVersion)
            }
        }
    }

    @Test
    fun `공백이 포함된 버전은 trim 후 파싱되어야 한다`() {
        // given & when
        val version = SemanticVersion.parse("  1.2.3  ")

        // then
        assertEquals(1, version.major)
        assertEquals(2, version.minor)
        assertEquals(3, version.patch)
    }

    @Test
    fun `버전 비교가 올바르게 동작해야 한다`() {
        // given
        val v1_2_3 = SemanticVersion(1, 2, 3)
        val v1_2_4 = SemanticVersion(1, 2, 4)
        val v1_3_0 = SemanticVersion(1, 3, 0)
        val v2_0_0 = SemanticVersion(2, 0, 0)

        // then - patch 비교
        assertTrue(v1_2_4 > v1_2_3)
        assertTrue(v1_2_3 < v1_2_4)

        // then - minor 비교
        assertTrue(v1_3_0 > v1_2_3)
        assertTrue(v1_2_3 < v1_3_0)

        // then - major 비교
        assertTrue(v2_0_0 > v1_3_0)
        assertTrue(v1_3_0 < v2_0_0)

        // then - 동일한 버전
        assertEquals(0, v1_2_3.compareTo(SemanticVersion(1, 2, 3)))
    }

    @Test
    fun `부등호 연산자들이 올바르게 동작해야 한다`() {
        // given
        val v1_2_3 = SemanticVersion(1, 2, 3)
        val v1_2_4 = SemanticVersion(1, 2, 4)
        val v1_2_3_copy = SemanticVersion(1, 2, 3)

        // then
        assertTrue(v1_2_4 > v1_2_3)
        assertTrue(v1_2_4 >= v1_2_3)
        assertTrue(v1_2_3 >= v1_2_3_copy)

        assertTrue(v1_2_3 < v1_2_4)
        assertTrue(v1_2_3 <= v1_2_4)
        assertTrue(v1_2_3 <= v1_2_3_copy)

        assertFalse(v1_2_3 > v1_2_4)
        assertFalse(v1_2_4 < v1_2_3)
    }

    @Test
    fun `toString이 올바른 형식을 반환해야 한다`() {
        // given
        val version = SemanticVersion(1, 2, 3)

        // when
        val versionString = version.toString()

        // then
        assertEquals("1.2.3", versionString)
    }

    @Test
    fun `major 버전 우선순위가 가장 높아야 한다`() {
        // given
        val v1_9_9 = SemanticVersion(1, 9, 9)
        val v2_0_0 = SemanticVersion(2, 0, 0)

        // then
        assertTrue(v2_0_0 > v1_9_9)
    }

    @Test
    fun `minor 버전 우선순위가 patch보다 높아야 한다`() {
        // given
        val v1_2_9 = SemanticVersion(1, 2, 9)
        val v1_3_0 = SemanticVersion(1, 3, 0)

        // then
        assertTrue(v1_3_0 > v1_2_9)
    }
}
