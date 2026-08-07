package com.example.shijian2.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.interaction.MutableInteractionSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = if (value.isNotEmpty()) {
            try {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(value)?.time
            } catch (e: Exception) {
                null
            }
        } else {
            System.currentTimeMillis()
        }
    )

    OutlinedTextField(
        value = value,
        onValueChange = {},
        modifier = modifier,
        label = { Text(label) },
        readOnly = true,
        trailingIcon = {
            IconButton(onClick = { showDatePicker = true }) {
                Icon(Icons.Default.CalendarToday, contentDescription = "选择日期")
            }
        },
        interactionSource = remember { MutableInteractionSource() }
    )

    if (value.isNotEmpty()) {
        LaunchedEffect(value) {
            try {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(value)?.time?.let {
                    datePickerState.selectedDateMillis = it
                }
            } catch (_: Exception) {}
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            onValueChange(sdf.format(Date(millis)))
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
            DatePicker(
                state = datePickerState,
                showModeToggle = false
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerFieldForExpense(
    value: String?,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = if (!value.isNullOrEmpty()) {
            try {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(value)?.time
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
        } else {
            System.currentTimeMillis()
        }
    )

    OutlinedTextField(
        value = value ?: "",
        onValueChange = {},
        modifier = modifier,
        label = { Text(label) },
        readOnly = true,
        trailingIcon = {
            IconButton(onClick = { showDatePicker = true }) {
                Icon(Icons.Default.CalendarToday, contentDescription = "选择日期")
            }
        },
        interactionSource = remember { MutableInteractionSource() }
    )

    if (!value.isNullOrEmpty()) {
        LaunchedEffect(value) {
            try {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(value)?.time?.let {
                    datePickerState.selectedDateMillis = it
                }
            } catch (_: Exception) {}
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            onValueChange(sdf.format(Date(millis)))
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
            DatePicker(
                state = datePickerState,
                showModeToggle = false
            )
        }
    }
}