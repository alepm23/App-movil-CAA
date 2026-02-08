package com.pictofly.data.model

import androidx.compose.ui.graphics.Color

data class LocalCategory(
    val id: String,
    val name: String,
    val imagePath: String,
    val pictogramCount: Int,
    val color: String? = null,
    val createdAt: Long? = null
) {
    fun toComposeColor(): Color {
        return Color(android.graphics.Color.parseColor(color))
    }

    val hasImage: Boolean
        get() = imagePath.isNotEmpty()
}