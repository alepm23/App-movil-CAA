package com.pictofly.data.model

data class LocalPictogram(
    val id: String = "",
    val categoryId: String,
    val name: String,
    val imagePath: String,
    val soundPath: String? = null,
    val type: String = "subject",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun isSubject(): Boolean = type == "subject"
    fun isVerb(): Boolean = type == "verb"
}