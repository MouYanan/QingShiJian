package com.example.shijian2.ui.todo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.shijian2.data.Todo
import com.example.shijian2.data.TodoRepository
import com.example.shijian2.util.DateUtil
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoDetailScreen(
    todo: Todo,
    onBack: () -> Unit,
    onEdit: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { TodoRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var isCompleted by remember { mutableStateOf(todo.status == "completed") }
    var remainingTime by remember { mutableStateOf(DateUtil.getRemainingTimeString(todo.dueDate)) }

    fun toggleStatus() {
        val updatedTodo = todo.copy(status = if (isCompleted) "pending" else "completed")
        coroutineScope.launch {
            repository.updateTodo(updatedTodo)
        }
        isCompleted = !isCompleted
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("任务标题", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = todo.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("任务状态", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isCompleted) "已完成" else "未完成",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Switch(
                            checked = isCompleted,
                            onCheckedChange = { toggleStatus() }
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("时间信息", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("开始时间", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = DateUtil.formatDateTimeForDisplay(todo.startDate),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("截止时间", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = DateUtil.formatDateTimeForDisplay(todo.dueDate),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("剩余时间", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = remainingTime,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (remainingTime.contains("已过期")) Color.Red else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("优先级", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = when (todo.priority) {
                            "high" -> "高优先级"
                            "medium" -> "中优先级"
                            "low" -> "低优先级"
                            else -> "未知"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = when (todo.priority) {
                            "high" -> Color.Red
                            "medium" -> Color(0xFFFFA500)
                            "low" -> Color.Green
                            else -> Color.Gray
                        }
                    )
                }
            }

            if (todo.description != null && todo.description!!.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("任务描述", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = todo.description!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF374151)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("返回")
                }
                Button(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("修改任务")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}