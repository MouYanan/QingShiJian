package com.example.shijian2.data

import kotlinx.serialization.Serializable

@Serializable
data class Note(
    val id: String = "",
    val title: String,
    val content: String,
    val createdAt: String,
    val updatedAt: String? = null
) {
    val identifier: String get() = if (id.isNotEmpty()) id else createdAt
}
