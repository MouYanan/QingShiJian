package com.example.shijian2.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.shijian2.data.BillRepository
import com.example.shijian2.data.BirthdayRepository
import com.example.shijian2.data.NoteRepository
import com.example.shijian2.data.TodoRepository
import com.example.shijian2.service.NotificationService
import com.example.shijian2.ui.common.SuccessMessage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showSuccessMessage by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("操作成功！") }

    // 通知权限请求
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        successMessage = if (granted) "通知权限已开启" else "通知权限被拒绝，可在系统设置中手动开启"
        showSuccessMessage = true
    }

    // 检查当前通知权限状态（动态更新）
    var isNotificationGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    // 从设置页返回时刷新权限状态
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    isNotificationGranted = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            // 旧版本直接打开应用设置页
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
            context.startActivity(intent)
            successMessage = "请在设置中开启通知权限"
            showSuccessMessage = true
        }
    }

    fun requestStoragePermission() {
        // 打开应用设置页让用户手动授权
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
        successMessage = "请在设置中开启存储权限"
        showSuccessMessage = true
    }
    
    fun testNotification() {
        val notificationService = NotificationService(context)
        notificationService.sendTodoNotification(
            com.example.shijian2.data.Todo(
                id = "test",
                title = "测试通知",
                startDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()),
                dueDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()),
                priority = "medium",
                createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            )
        )
        successMessage = "测试通知已发送"
        showSuccessMessage = true
    }
    
    fun backupData() {
        coroutineScope.launch {
            val billsRepository = BillRepository(context)
            val todosRepository = TodoRepository(context)
            val notesRepository = NoteRepository(context)
            val birthdayRepository = BirthdayRepository(context)
            
            try {
                val bills = billsRepository.getAllBills().first()
                val todos = todosRepository.getAllTodos().first()
                val notes = notesRepository.getAllNotes().first()
                val birthdays = birthdayRepository.getAllBirthdays().first()
                
                val backupJson = kotlinx.serialization.json.Json.encodeToString(
                    kotlinx.serialization.serializer<Map<String, kotlin.Any>>(),
                    mapOf(
                        "bills" to bills,
                        "todos" to todos,
                        "notes" to notes,
                        "birthdays" to birthdays
                    )
                )
                
                val fileName = "backup_${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}.json"
                val backupDir = File(context.getExternalFilesDir(null), "Backup")
                backupDir.mkdirs()
                val backupFile = File(backupDir, fileName)
                
                val outputStream = FileOutputStream(backupFile)
                outputStream.write(backupJson.toByteArray())
                outputStream.close()
                successMessage = "数据备份成功，文件保存为: $fileName"
            } catch (e: Exception) {
                successMessage = "数据备份失败: ${e.message}"
            }
            
            showSuccessMessage = true
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("权限管理") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Text("权限管理", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("管理应用所需的权限", style = MaterialTheme.typography.bodyMedium)
            }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("通知权限", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "用于待办事项和生日管家的提醒功能",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Text(
                            if (isNotificationGranted) "当前状态：已授权" else "当前状态：未授权",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isNotificationGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { requestNotificationPermission() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = "通知", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isNotificationGranted) "管理通知权限" else "请求通知权限")
                    }
                }
            }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("存储权限", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "用于备份和恢复数据", 
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { requestStoragePermission() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "存储", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("请求存储权限")
                    }
                }
            }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("测试通知", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "测试通知功能是否正常", 
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { testNotification() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "测试通知", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("发送测试通知")
                    }
                }
            }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("备份数据", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "将数据备份到本地存储", 
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { backupData() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "备份", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("备份数据")
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("返回设置")
            }
        }
    }
    
    SuccessMessage(
        message = successMessage,
        isVisible = showSuccessMessage,
        onDismiss = { showSuccessMessage = false }
    )
}
