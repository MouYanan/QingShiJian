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

/**
 * 将 Double 四舍五入到2位小数，避免浮点求和累积精度误差（如 914.8199999999999）
 */
fun Double.roundTo2Decimals(): Double =
    kotlin.math.round(this * 100) / 100.0

/**
 * 将金额格式化为简洁字符串显示（去掉多余的0），如 100.50 → "100.5"，100.00 → "100"
 */
fun Double.formatAmount(): String =
    "%.2f".format(this).trimEnd('0').trimEnd('.')
