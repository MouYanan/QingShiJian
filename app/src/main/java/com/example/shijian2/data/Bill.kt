package com.example.shijian2.data

import kotlinx.serialization.Serializable

@Serializable
sealed class Bill {
    abstract val id: String
    abstract val total: Double
    abstract val expenses: List<Expense>
    abstract val createdAt: String

    val identifier: String get() = if (id.isNotEmpty()) id else createdAt
}

@Serializable
data class TimeBill(
    override val id: String = "",
    val date: String,
    override val expenses: List<Expense>,
    override val total: Double,
    override val createdAt: String
) : Bill()

@Serializable
data class ProjectBill(
    override val id: String = "",
    val name: String,
    override val expenses: List<Expense>,
    override val total: Double,
    override val createdAt: String
) : Bill()

@Serializable
data class Expense(
    val item: String,
    val amount: Double,
    val date: String? = null
)
