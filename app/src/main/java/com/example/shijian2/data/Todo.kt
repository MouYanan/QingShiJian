package com.example.shijian2.data

import kotlinx.serialization.Serializable

@Serializable
data class Todo(
    val id: String = "",
    val title: String,
    val description: String? = null,
    val startDate: String,
    val dueDate: String,
    val priority: String,
    val status: String = "pending",
    val createdAt: String,
    val reminderHours: Int = 4,
    val reminderMinutes: Int = 0
) {
    val identifier: String get() = if (id.isNotEmpty()) id else createdAt
}