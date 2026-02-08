package com.pictofly.data.model

data class Pictogram(
    val name: String,
    val imageUrl: String,
    val isLocal: Boolean = false,
    val localImagePath: String = "",
    val localFileUri: String? = null,
    val isPredefined: Boolean = false,
    val isVisible: Boolean = true,
    val createdByUser: Boolean = false
) {

    fun getDisplayImageUrl(): String {
        return if (isLocal && imageUrl.isNotEmpty()) {
            imageUrl
        } else {
            imageUrl
        }
    }
}
