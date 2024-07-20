package notbe.tmtm.ddanddanserver.common.util

import org.junit.jupiter.api.Assertions.assertNotNull
import kotlin.test.Test
import kotlin.test.assertEquals

class TsidUtilsTest {
    @Test
    fun tsid() {
        val tsid = generateTsid()
        assertNotNull(tsid)
        assertEquals(tsid.length, 13)
    }
}
