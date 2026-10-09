package com.example.shijian2.ui.bill

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shijian2.data.Bill
import com.example.shijian2.data.Expense
import com.example.shijian2.data.ProjectBill
import com.example.shijian2.data.TimeBill
import com.example.shijian2.data.formatAmount
import com.example.shijian2.data.roundTo2Decimals
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 汇总统计数据类
 */
data class SummaryData(
    val selectedBills: List<Bill>
) {
    /** 所有支出明细（展平） */
    val allExpenses: List<Pair<Bill, Expense>>
        get() = selectedBills.flatMap { bill ->
            bill.expenses.map { expense -> bill to expense }
        }

    /** 选中条目数 */
    val count: Int get() = allExpenses.size

    /** 合计总金额 */
    val totalAmount: Double
        get() = allExpenses.sumOf { it.second.amount }.roundTo2Decimals()

    /** 单笔最大值及对应账单名称 */
    val maxExpense: Pair<Expense, String>?
        get() = allExpenses.maxByOrNull { it.second.amount }?.let { (bill, exp) ->
            exp to getBillTitle(bill)
        }

    /** 单笔最小值及对应账单名称 */
    val minExpense: Pair<Expense, String>?
        get() = allExpenses.minByOrNull { it.second.amount }?.let { (bill, exp) ->
            exp to getBillTitle(bill)
        }

    /** 平均值 */
    val average: Double
        get() = if (count > 0) (totalAmount / count).roundTo2Decimals() else 0.0

    /** 所有日期字符串 */
    private val allDates: List<String>
        get() = allExpenses.mapNotNull { (bill, exp) ->
            exp.date ?: getBillDate(bill)
        }.distinct()

    /** 最早日期 */
    val earliestDate: String? get() = allDates.minByOrNull { it }

    /** 最晚日期 */
    val latestDate: String? get() = allDates.maxByOrNull { it }

    /** 时间跨度天数（相隔 N 天） */
    val daySpan: Int
        get() {
            val dates = allDates.mapNotNull { runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it) }.getOrNull() }
            if (dates.size < 2) return 0
            val diff = dates.max().time - dates.min().time
            return (diff / (1000 * 60 * 60 * 24)).toInt()
        }

    /** 日均金额 */
    val dailyAverage: Double
        get() = if (daySpan > 0) (totalAmount / daySpan).roundTo2Decimals() else totalAmount

    /** 明细列表（按金额降序），含日期 */
    val sortedExpenses: List<Triple<Expense, String, String>>
        get() = allExpenses
            .sortedByDescending { it.second.amount }
            .map { (bill, exp) -> Triple(exp, getBillTitle(bill), exp.date ?: getBillDate(bill)) }
}

private fun getBillTitle(bill: Bill): String = when (bill) {
    is ProjectBill -> bill.name
    is TimeBill -> "时间账单 - ${bill.date}"
}

private fun getBillDate(bill: Bill): String = when (bill) {
    is ProjectBill -> bill.createdAt.takeIf { it.isNotEmpty() } ?: ""
    is TimeBill -> bill.date
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    selectedBills: List<Bill>,
    onBack: () -> Unit,
    onReselect: () -> Unit
) {
    val context = LocalContext.current
    val data = remember(selectedBills) { SummaryData(selectedBills) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("汇总统计", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        copySummaryToClipboard(context, data)
                        Toast.makeText(context, "已复制汇总信息", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "复制统计文本")
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = onReselect,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("重新选择")
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(0.dp))

            // 第一区：核心概览区
            SummaryOverviewCard(data)

            // 第二区：指标卡片区
            MetricCardsRow(data)

            // 第三区：明细列表区
            DetailListSection(data)

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SummaryOverviewCard(data: SummaryData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "选中条目：${data.count} 条",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "¥${String.format("%.2f", data.totalAmount)}",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )
            if (data.earliestDate != null && data.latestDate != null) {
                Text(
                    text = "${data.earliestDate} — ${data.latestDate}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "相隔 ${data.daySpan} 天",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun MetricCardsRow(data: SummaryData) {
    val maxExp = data.maxExpense
    val minExp = data.minExpense

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetricCard(
            title = "单笔最大值",
            amount = maxExp?.first?.amount ?: 0.0,
            subtitle = maxExp?.second ?: "—",
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            title = "单笔最小值",
            amount = minExp?.first?.amount ?: 0.0,
            subtitle = minExp?.second ?: "—",
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(Modifier.height(0.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetricCard(
            title = "平均值",
            amount = data.average,
            subtitle = "总金额 ÷ ${data.count} 条",
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            title = "日均金额",
            amount = data.dailyAverage,
            subtitle = if (data.daySpan > 0) "总金额 ÷ ${data.daySpan} 天" else "—",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MetricCard(
    title: String,
    amount: Double,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "¥${String.format("%.2f", amount)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DetailListSection(data: SummaryData) {
    Text(
        text = "选中明细",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp)
    )
    HorizontalDivider()
    data.sortedExpenses.forEach { (expense, billTitle, date) ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.item.ifEmpty { billTitle },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
                if (date.isNotEmpty()) {
                    Text(
                        text = date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = billTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "¥${String.format("%.2f", expense.amount)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

private fun copySummaryToClipboard(context: Context, data: SummaryData) {
    val sb = StringBuilder()
    sb.appendLine("===== 汇总统计 =====")
    sb.appendLine("选中条目：${data.count} 条")
    sb.appendLine("合计总金额：¥${String.format("%.2f", data.totalAmount)}")
    if (data.earliestDate != null && data.latestDate != null) {
        sb.appendLine("时间区间：${data.earliestDate} — ${data.latestDate}")
        sb.appendLine("时间跨度：相隔 ${data.daySpan} 天")
    }
    sb.appendLine()
    data.maxExpense?.let { (exp, title) ->
        sb.appendLine("单笔最大值：¥${String.format("%.2f", exp.amount)}（$title - ${exp.item}）")
    }
    data.minExpense?.let { (exp, title) ->
        sb.appendLine("单笔最小值：¥${String.format("%.2f", exp.amount)}（$title - ${exp.item}）")
    }
    sb.appendLine("平均值：¥${String.format("%.2f", data.average)}")
    sb.appendLine("日均金额：¥${String.format("%.2f", data.dailyAverage)}")
    sb.appendLine()
    sb.appendLine("----- 选中明细 -----")
    data.sortedExpenses.forEach { (exp, title, date) ->
        val dateStr = if (date.isNotEmpty()) " [$date]" else ""
        sb.appendLine("${exp.item.ifEmpty { title }}$dateStr\t¥${String.format("%.2f", exp.amount)}")
    }

    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("汇总统计", sb.toString()))
}
