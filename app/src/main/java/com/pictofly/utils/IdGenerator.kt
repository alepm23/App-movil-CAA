package com.pictofly.utils

import java.util.UUID

object IdGenerator {
    fun generateId(): String = UUID.randomUUID().toString()

    fun generateImageFileName(prefix: String = "img"): String {
        return "${prefix}_${UUID.randomUUID().toString().substring(0, 8)}.jpg"
    }
}