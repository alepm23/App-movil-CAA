package com.pictofly.data.model

import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import java.io.File

data class Category(
    val name: String,
    val imageUrl: String,
    val isLocal: Boolean = false,
    val localImagePath: String = "",
    val localFileUri: String? = null
) {
    fun getDisplayImageUrl(): String {
        return if (isLocal && localFileUri != null) {
            localFileUri
        } else {
            imageUrl
        }
    }
}



