package echo.shared

import org.bson.types.ObjectId

fun String.toObjectIdOrNull(): ObjectId? = runCatching { ObjectId(this) }.getOrNull()
