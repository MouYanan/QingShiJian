package com.example.shijian2.ui.birthday

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.shijian2.data.Birthday
import com.example.shijian2.data.BirthdayRepository
import com.example.shijian2.util.LunarCalendarUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirthdayEntryScreen(
    onBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    birthdayToEdit: Birthday? = null
) {
    val context = LocalContext.current
    val repository = remember { BirthdayRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    var name by remember { mutableStateOf(birthdayToEdit?.name ?: "") }
    var date by remember { mutableStateOf(birthdayToEdit?.date ?: "") }
    var showSolarDatePicker by remember { mutableStateOf(false) }
    var showLunarDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )

    var lunarYear by remember { mutableStateOf(2000) }
    var lunarMonth by remember { mutableStateOf(1) }
    var lunarDay by remember { mutableStateOf(1) }

    LaunchedEffect(birthdayToEdit) {
        if (birthdayToEdit != null && birthdayToEdit.calendarType == "lunar") {
            val lunar = LunarCalendarUtil.solarToLunar(birthdayToEdit.date)
            lunarYear = lunar.first
            lunarMonth = lunar.second
            lunarDay = lunar.third
        }
    }

    var relation by remember { mutableStateOf(birthdayToEdit?.relation ?: "") }
    var calendarType by remember { mutableStateOf(birthdayToEdit?.calendarType ?: "solar") }
    var notes by remember { mutableStateOf(birthdayToEdit?.notes ?: "") }
    var showSuccess by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            delay(300)
            onSaveSuccess()
        }
    }

    fun saveBirthday() {
        if (name.isNotEmpty() && date.isNotEmpty() && !isSaving) {
            isSaving = true
            coroutineScope.launch {
                val birthday = if (birthdayToEdit != null) {
                    birthdayToEdit.copy(
                        name = name,
                        date = date,
                        relation = relation,
                        calendarType = calendarType,
                        notes = notes
                    )
                } else {
                    Birthday(
                        id = java.util.UUID.randomUUID().toString(),
                        name = name,
                        date = date,
                        relation = relation,
                        calendarType = calendarType,
                        notes = notes,
                        createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                    )
                }

                if (birthdayToEdit != null) {
                    repository.updateBirthday(birthday)
                } else {
                    repository.addBirthday(birthday)
                }
                showSuccess = true
                isSaving = false
            }
        }
    }

    if (showSolarDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showSolarDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(millis))
                        }
                        showSolarDatePicker = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSolarDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState, showModeToggle = false)
        }
    }

    if (showLunarDatePicker) {
        LunarDatePickerDialog(
            initialYear = lunarYear,
            initialMonth = lunarMonth,
            initialDay = lunarDay,
            onDismiss = { showLunarDatePicker = false },
            onConfirm = { year, month, day ->
                lunarYear = year
                lunarMonth = month
                lunarDay = day
                date = LunarCalendarUtil.lunarToSolar(year, month, day)
                showLunarDatePicker = false
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
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("姓名 *") },
                singleLine = true,
                enabled = !isSaving
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("日历类型", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FilterChip(
                    selected = calendarType == "solar",
                    onClick = {
                        calendarType = "solar"
                        date = ""
                    },
                    label = { Text("公历") },
                    modifier = Modifier.weight(1f),
                    enabled = !isSaving
                )
                FilterChip(
                    selected = calendarType == "lunar",
                    onClick = {
                        calendarType = "lunar"
                        date = ""
                    },
                    label = { Text("农历") },
                    modifier = Modifier.weight(1f),
                    enabled = !isSaving
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = if (calendarType == "lunar" && date.isNotEmpty()) {
                    val lunar = LunarCalendarUtil.solarToLunar(date)
                    LunarCalendarUtil.getLunarDateString(lunar.first, lunar.second, lunar.third)
                } else if (date.isNotEmpty()) {
                    date
                } else {
                    ""
                },
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (calendarType == "solar") {
                            showSolarDatePicker = true
                        } else {
                            showLunarDatePicker = true
                        }
                    },
                label = { Text("生日日期 *") },
                readOnly = true,
                enabled = false,
                trailingIcon = {
                    IconButton(onClick = {
                        if (calendarType == "solar") {
                            showSolarDatePicker = true
                        } else {
                            showLunarDatePicker = true
                        }
                    }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "选择日期")
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = relation,
                onValueChange = { relation = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("关系 (如：家人、朋友、同事等)") },
                singleLine = true,
                enabled = !isSaving
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                label = { Text("备注信息") },
                maxLines = 4,
                enabled = !isSaving
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
                    onClick = { saveBirthday() },
                    modifier = Modifier.weight(1f),
                    enabled = name.isNotEmpty() && date.isNotEmpty() && !isSaving
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

@Composable
fun LunarDatePickerDialog(
    initialYear: Int,
    initialMonth: Int,
    initialDay: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int, Int) -> Unit
) {
    var selectedYear by remember { mutableStateOf(initialYear) }
    var selectedMonth by remember { mutableStateOf(initialMonth) }
    var selectedDay by remember { mutableStateOf(initialDay) }

    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val years = (currentYear - 100..currentYear + 50).toList()
    val leapMonth = LunarCalendarUtil.getLeapMonth(selectedYear)

    val daysInMonth = if (selectedMonth == leapMonth) {
        LunarCalendarUtil.getLeapDays(selectedYear)
    } else {
        LunarCalendarUtil.monthDays(selectedYear, selectedMonth)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择农历日期") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("年份:", style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (selectedYear > years.first()) selectedYear-- }
                        ) {
                            Text("<", style = MaterialTheme.typography.titleLarge)
                        }
                        Text(
                            selectedYear.toString(),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.width(60.dp),
                            textAlign = TextAlign.Center
                        )
                        IconButton(
                            onClick = { if (selectedYear < years.last()) selectedYear++ }
                        ) {
                            Text(">", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("月份:", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                val monthNames = listOf("正月","二月","三月","四月","五月","六月","七月","八月","九月","十月","冬月","腊月")
                Column {
                    for (row in 0..2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (col in 0..3) {
                                val month = row * 4 + col + 1
                                if (month <= 12) {
                                    Box(
                                        modifier = Modifier
                                            .size(60.dp, 36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (selectedMonth == month) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.surface
                                            )
                                            .border(
                                                1.dp,
                                                if (selectedMonth == month) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.outline
                                            )
                                            .clickable { selectedMonth = month },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            monthNames[month - 1],
                                            color = if (selectedMonth == month) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurface,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("日期:", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))

                val dayNames = listOf("初一","初二","初三","初四","初五","初六","初七","初八","初九","初十",
                    "十一","十二","十三","十四","十五","十六","十七","十八","十九","二十",
                    "廿一","廿二","廿三","廿四","廿五","廿六","廿七","廿八","廿九","三十")

                Column {
                    for (row in 0..4) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (col in 0..5) {
                                val day = row * 6 + col + 1
                                if (day <= daysInMonth) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp, 36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (selectedDay == day) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.surface
                                            )
                                            .border(
                                                1.dp,
                                                if (selectedDay == day) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.outline
                                            )
                                            .clickable { selectedDay = day },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            dayNames[day - 1],
                                            color = if (selectedDay == day) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurface,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedYear, selectedMonth, selectedDay) }) {
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