package com.example.shijian2.ui.todo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.shijian2.data.Todo
import com.example.shijian2.data.TodoRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoEntryScreen(
    onBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    todoToEdit: Todo? = null
) {
    val context = LocalContext.current
    val repository = remember { TodoRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val now = Calendar.getInstance()
    val defaultStartDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(now.time)
    val defaultDueDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(now.timeInMillis + 24 * 60 * 60 * 1000))

    var title by remember { mutableStateOf(todoToEdit?.title ?: "") }
    var description by remember { mutableStateOf(todoToEdit?.description ?: "") }
    var startDate by remember { mutableStateOf(todoToEdit?.startDate ?: defaultStartDate) }
    var dueDate by remember { mutableStateOf(todoToEdit?.dueDate ?: defaultDueDate) }
    var priority by remember { mutableStateOf(todoToEdit?.priority ?: "medium") }
    var status by remember { mutableStateOf(todoToEdit?.status ?: "pending") }
    var reminderHours by remember { mutableStateOf(todoToEdit?.reminderHours ?: 4) }
    var reminderMinutes by remember { mutableStateOf(todoToEdit?.reminderMinutes ?: 0) }
    var showSuccess by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showDueDatePicker by remember { mutableStateOf(false) }
    var showDueTimePicker by remember { mutableStateOf(false) }

    val startDatePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
    val dueDatePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis() + 24 * 60 * 60 * 1000)
    val startTimePickerState = rememberTimePickerState(initialHour = now.get(Calendar.HOUR_OF_DAY), initialMinute = now.get(Calendar.MINUTE))
    val dueTimePickerState = rememberTimePickerState(initialHour = now.get(Calendar.HOUR_OF_DAY), initialMinute = now.get(Calendar.MINUTE))

    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            delay(300)
            onSaveSuccess()
        }
    }

    fun saveTodo() {
        if (title.isNotEmpty() && !isSaving) {
            isSaving = true
            coroutineScope.launch {
                val todo = if (todoToEdit != null) {
                    todoToEdit.copy(
                        title = title,
                        description = description,
                        startDate = startDate,
                        dueDate = dueDate,
                        priority = priority,
                        status = status,
                        reminderHours = reminderHours,
                        reminderMinutes = reminderMinutes
                    )
                } else {
                    Todo(
                        id = java.util.UUID.randomUUID().toString(),
                        title = title,
                        description = description,
                        startDate = startDate,
                        dueDate = dueDate,
                        priority = priority,
                        status = status,
                        createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()),
                        reminderHours = reminderHours,
                        reminderMinutes = reminderMinutes
                    )
                }

                if (todoToEdit != null) {
                    repository.updateTodo(todo)
                } else {
                    repository.addTodo(todo)
                }
                showSuccess = true
                isSaving = false
            }
        }
    }

    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        startDatePickerState.selectedDateMillis?.let { millis ->
                            val cal = Calendar.getInstance()
                            cal.timeInMillis = millis
                            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
                            val parts = startDate.split(" ")
                            startDate = if (parts.size > 1) "$dateStr ${parts[1]}" else "$dateStr 00:00"
                        }
                        showStartDatePicker = false
                        showStartTimePicker = true
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = startDatePickerState, showModeToggle = false)
        }
    }

    if (showStartTimePicker) {
        TimePickerDialog(
            onDismiss = { showStartTimePicker = false },
            onConfirm = { hour, minute ->
                val parts = startDate.split(" ")
                startDate = "${parts[0]} %02d:%02d".format(hour, minute)
                showStartTimePicker = false
            }
        )
    }

    if (showDueDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDueDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        dueDatePickerState.selectedDateMillis?.let { millis ->
                            val cal = Calendar.getInstance()
                            cal.timeInMillis = millis
                            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
                            val parts = dueDate.split(" ")
                            dueDate = if (parts.size > 1) "$dateStr ${parts[1]}" else "$dateStr 00:00"
                        }
                        showDueDatePicker = false
                        showDueTimePicker = true
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDueDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = dueDatePickerState, showModeToggle = false)
        }
    }

    if (showDueTimePicker) {
        TimePickerDialog(
            onDismiss = { showDueTimePicker = false },
            onConfirm = { hour, minute ->
                val parts = dueDate.split(" ")
                dueDate = "${parts[0]} %02d:%02d".format(hour, minute)
                showDueTimePicker = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("待办事项标题 *") },
                singleLine = true,
                enabled = !isSaving
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp),
                label = { Text("详细描述") },
                minLines = 4,
                maxLines = 10,
                enabled = !isSaving
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = startDate,
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showStartDatePicker = true },
                label = { Text("开始时间") },
                readOnly = true,
                enabled = false,
                trailingIcon = {
                    Row {
                        IconButton(onClick = { showStartDatePicker = true }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "选择日期")
                        }
                        IconButton(onClick = { showStartTimePicker = true }) {
                            Icon(Icons.Default.AccessTime, contentDescription = "选择时间")
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = dueDate,
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDueDatePicker = true },
                label = { Text("截止时间") },
                readOnly = true,
                enabled = false,
                trailingIcon = {
                    Row {
                        IconButton(onClick = { showDueDatePicker = true }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "选择日期")
                        }
                        IconButton(onClick = { showDueTimePicker = true }) {
                            Icon(Icons.Default.AccessTime, contentDescription = "选择时间")
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("优先级", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FilterChip(
                    selected = priority == "high",
                    onClick = { priority = "high" },
                    label = { Text("高") },
                    enabled = !isSaving,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.error,
                        selectedLabelColor = MaterialTheme.colorScheme.onError
                    )
                )
                FilterChip(
                    selected = priority == "medium",
                    onClick = { priority = "medium" },
                    label = { Text("中") },
                    enabled = !isSaving
                )
                FilterChip(
                    selected = priority == "low",
                    onClick = { priority = "low" },
                    label = { Text("低") },
                    enabled = !isSaving,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("任务状态", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FilterChip(
                    selected = status == "pending",
                    onClick = { status = "pending" },
                    label = { Text("未完成") },
                    enabled = !isSaving
                )
                FilterChip(
                    selected = status == "completed",
                    onClick = { status = "completed" },
                    label = { Text("已完成") },
                    enabled = !isSaving,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("提醒设置", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = reminderHours.toString(),
                    onValueChange = { 
                        val hours = it.toIntOrNull() ?: 0
                        if (hours >= 0 && hours <= 24) {
                            reminderHours = hours
                        }
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text("提醒小时") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = !isSaving
                )
                OutlinedTextField(
                    value = reminderMinutes.toString(),
                    onValueChange = { 
                        val minutes = it.toIntOrNull() ?: 0
                        if (minutes >= 0 && minutes < 60) {
                            reminderMinutes = minutes
                        }
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text("提醒分钟") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = !isSaving
                )
            }
            Text(
                text = "将在距离截止时间还有${reminderHours}小时${reminderMinutes}分钟时发送提醒",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(32.dp))

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
                    onClick = { saveTodo() },
                    modifier = Modifier.weight(1f),
                    enabled = title.isNotEmpty() && !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("保存")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val timePickerState = rememberTimePickerState()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择时间") },
        text = {
            TimePicker(state = timePickerState)
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(timePickerState.hour, timePickerState.minute) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}