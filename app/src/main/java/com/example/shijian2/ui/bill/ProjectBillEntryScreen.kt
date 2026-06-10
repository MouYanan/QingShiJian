package com.example.shijian2.ui.bill

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.shijian2.data.BillRepository
import com.example.shijian2.data.Expense
import com.example.shijian2.data.ProjectBill
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectBillEntryScreen(
    onBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    billToEdit: ProjectBill? = null
) {
    val context = LocalContext.current
    val repository = remember { BillRepository(context) }
    val coroutineScope = rememberCoroutineScope()

    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    var projectName by remember { mutableStateOf(billToEdit?.name ?: "") }
    var expenses by remember { mutableStateOf(billToEdit?.expenses ?: listOf(
        Expense("购买材料", 0.0, today),
        Expense("人工费用", 0.0, today)
    )) }
    var total by remember { mutableStateOf(billToEdit?.total ?: 0.0) }
    var isSaving by remember { mutableStateOf(false) }

    fun calculateTotal() {
        total = expenses.sumOf { it.amount }
    }

    fun addExpense() {
        expenses = expenses + Expense("新事项", 0.0, today)
    }

    fun removeExpense(index: Int) {
        if (expenses.size > 1) {
            expenses = expenses.filterIndexed { i, _ -> i != index }
            calculateTotal()
        }
    }

    fun updateExpenseDate(index: Int, date: String) {
        expenses = expenses.mapIndexed { i, expense ->
            if (i == index) expense.copy(date = date) else expense
        }
    }

    fun updateExpenseItem(index: Int, item: String) {
        expenses = expenses.mapIndexed { i, expense ->
            if (i == index) expense.copy(item = item) else expense
        }
    }

    fun updateExpenseAmount(index: Int, amount: Double) {
        expenses = expenses.mapIndexed { i, expense ->
            if (i == index) expense.copy(amount = amount) else expense
        }
        calculateTotal()
    }

    fun saveBill() {
        if (projectName.isNotEmpty() && expenses.any { it.amount > 0 } && !isSaving) {
            isSaving = true
            val bill = if (billToEdit != null) {
                billToEdit.copy(
                    name = projectName,
                    expenses = expenses.filter { it.amount > 0 },
                    total = total
                )
            } else {
                ProjectBill(
                    id = java.util.UUID.randomUUID().toString(),
                    name = projectName,
                    expenses = expenses.filter { it.amount > 0 },
                    total = total,
                    createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                )
            }

            coroutineScope.launch {
                if (billToEdit != null) {
                    repository.updateBill(bill)
                } else {
                    repository.addBill(bill)
                }
                delay(300)
                isSaving = false
                onSaveSuccess()
            }
        }
    }

    LaunchedEffect(expenses) {
        calculateTotal()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("项目名称") }
                )
            }

            item {
                Text("支出明细", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
            }

            itemsIndexed(expenses) { index, expense ->
                ExpenseItemWithDatePicker(
                    expense = expense,
                    onDateChange = { updateExpenseDate(index, it) },
                    onItemChange = { updateExpenseItem(index, it) },
                    onAmountChange = { updateExpenseAmount(index, it) },
                    onRemove = { if (expenses.size > 1) removeExpense(index) },
                    showRemoveButton = expenses.size > 1
                )
            }

            item {
                TextButton(
                    onClick = { addExpense() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("添加明细")
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("总金额:", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "¥%.2f".format(total),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f),
                        enabled = !isSaving
                    ) {
                        Text("返回")
                    }
                    Button(
                        onClick = { saveBill() },
                        modifier = Modifier.weight(1f),
                        enabled = projectName.isNotEmpty() && expenses.any { it.amount > 0 } && !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("保存账单")
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseItemWithDatePicker(
    expense: Expense,
    onDateChange: (String) -> Unit,
    onItemChange: (String) -> Unit,
    onAmountChange: (Double) -> Unit,
    onRemove: () -> Unit,
    showRemoveButton: Boolean
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onDateChange(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(millis)))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState, showModeToggle = false)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = expense.date ?: "",
                onValueChange = {},
                modifier = Modifier
                    .weight(1f)
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
            if (showRemoveButton) {
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = expense.item,
                onValueChange = onItemChange,
                modifier = Modifier.weight(1f),
                label = { Text("事项") }
            )
            OutlinedTextField(
                value = if (expense.amount == 0.0) "" else expense.amount.toString(),
                onValueChange = { onAmountChange(it.toDoubleOrNull() ?: 0.0) },
                modifier = Modifier.weight(1f),
                label = { Text("金额") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        }
    }
}