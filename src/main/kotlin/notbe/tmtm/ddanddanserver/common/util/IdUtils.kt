package notbe.tmtm.ddanddanserver.common.util

import org.bson.types.ObjectId

fun generateObjectId(): String = ObjectId.get().toString()
