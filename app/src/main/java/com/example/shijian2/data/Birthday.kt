package com.example.shijian2.data

import kotlinx.serialization.Serializable

@Serializable
data class Birthday(
    val id: String = "",
    val name: String,
    val date: String,
    val relation: String? = null,
    val calendarType: String = "solar",
    val notes: String? = null,
    val createdAt: String
) {
    val identifier: String get() = if (id.isNotEmpty()) id else createdAt
}
