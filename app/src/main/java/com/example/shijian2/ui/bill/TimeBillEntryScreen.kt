package com.example.shijian2.ui.bill

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.shijian2.data.*
import com.example.shijian2.util.DateUtil
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeBillEntryScreen(
    onBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    billToEdit: TimeBill? = null
) {
    val context = LocalContext.current
    val repository = remember { BillRepository(context) }
    val coroutineScope = rememberCoroutineScope()

    // 使用当前日期作为默认值
    val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    var date by remember { mutableStateOf(currentDate) }
    var expenses by remember { mutableStateOf(mutableListOf(Expense("", 0.0, date))) }
    var showDatePicker by remember { mutableStateOf(false) }

    // 如果是编辑模式，初始化数据
    LaunchedEffect(billToEdit) {
        if (billToEdit != null) {
            date = billToEdit.date
            expenses = billToEdit.expenses.toMutableList()
        }
    }

    fun calculateTotal(): Double {
        return expenses.sumOf { it.amount }
    }

    fun saveBill() {
        val total = calculateTotal()
        val timeBill = if (billToEdit != null) {
            billToEdit.copy(
                date = date,
                expenses = expenses,
                total = total
            )
        } else {
            TimeBill(
                id = java.util.UUID.randomUUID().toString(),
                date = date,
                expenses = expenses,
                total = total,
                createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            )
        }

        coroutineScope.launch {
            if (billToEdit != null) {
                repository.updateBill(timeBill)
            } else {
                repository.addBill(timeBill)
            }
            onSaveSuccess()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        item {
            Column {
                Text("选择日期", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = DateUtil.formatDateForDisplay(date),
                    onValueChange = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    label = { Text("日期") },
                    readOnly = true,
                    enabled = false,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "选择日期")
                        }
                    }
                )
            }
        }

        item {
            Text("支出项目", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
        }

        itemsIndexed(expenses) { index, expense ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = expense.item,
                    onValueChange = { newItem ->
                        val updatedExpenses = expenses.toMutableList()
                        updatedExpenses[index] = expense.copy(item = newItem)
                        expenses = updatedExpenses
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text("项目") }
                )

                OutlinedTextField(
                    value = if (expense.amount == 0.0) "" else expense.amount.toString(),
                    onValueChange = { newAmount ->
                        val amount = newAmount.toDoubleOrNull() ?: 0.0
                        val updatedExpenses = expenses.toMutableList()
                        updatedExpenses[index] = expense.copy(amount = amount)
                        expenses = updatedExpenses
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text("金额") }
                )
            }
        }

        item {
            Button(
                onClick = {
                    expenses = expenses.toMutableList().apply {
                        add(Expense("", 0.0, date))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加项目")
                Spacer(modifier = Modifier.width(8.dp))
                Text("添加支出项目")
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("总计", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "¥${String.format("%.2f", calculateTotal())}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("取消")
                }
                Button(
                    onClick = { saveBill() },
                    modifier = Modifier.weight(1f)
                ) {
                    if (billToEdit != null) {
                        Text("更新账单")
                    } else {
                        Text("保存账单")
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)?.time
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(millis))
                            date = selectedDate
                            expenses = expenses.map { expense ->
                                expense.copy(date = date)
                            }.toMutableList()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showDatePicker = false }
                ) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}