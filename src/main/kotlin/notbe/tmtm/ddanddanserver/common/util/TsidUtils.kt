package notbe.tmtm.ddanddanserver.common.util

import com.github.f4b6a3.tsid.TsidCreator

fun generateTsid(): String = TsidCreator.getTsid().toString()
