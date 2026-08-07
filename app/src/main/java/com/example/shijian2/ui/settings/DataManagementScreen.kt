package com.example.shijian2.ui.settings

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.shijian2.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
data class BackupData(
    val bills: List<TimeBill> = emptyList(),
    val projectBills: List<ProjectBill> = emptyList(),
    val todos: List<Todo> = emptyList(),
    val notes: List<Note> = emptyList(),
    val birthdays: List<Birthday> = emptyList(),
    val backupDate: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataManagementScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportSelectionDialog by remember { mutableStateOf(false) }
    var showResultDialog by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf("") }
    var isSuccess by remember { mutableStateOf(true) }

    var exportBills by remember { mutableStateOf(true) }
    var exportProjectBills by remember { mutableStateOf(true) }
    var exportTodos by remember { mutableStateOf(true) }
    var exportNotes by remember { mutableStateOf(true) }
    var exportBirthdays by remember { mutableStateOf(true) }

    var importBills by remember { mutableStateOf(true) }
    var importProjectBills by remember { mutableStateOf(true) }
    var importTodos by remember { mutableStateOf(true) }
    var importNotes by remember { mutableStateOf(true) }
    var importBirthdays by remember { mutableStateOf(true) }

    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            selectedFileUri = it
            selectedFileName = it.lastPathSegment ?: "backup.json"
            showImportSelectionDialog = true
        }
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("选择导出内容") },
            text = {
                Column {
                    Text(
                        "请选择要导出的数据类型：",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = exportBills, onCheckedChange = { exportBills = it })
                        Text("时间账单")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = exportProjectBills, onCheckedChange = { exportProjectBills = it })
                        Text("项目账单")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = exportTodos, onCheckedChange = { exportTodos = it })
                        Text("待办事项")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = exportNotes, onCheckedChange = { exportNotes = it })
                        Text("笔记")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = exportBirthdays, onCheckedChange = { exportBirthdays = it })
                        Text("生日管家")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExportDialog = false
                        isExporting = true
                        coroutineScope.launch {
                            try {
                                val result = performExport(
                                    context,
                                    exportBills,
                                    exportProjectBills,
                                    exportTodos,
                                    exportNotes,
                                    exportBirthdays
                                )
                                isSuccess = true
                                resultMessage = result
                            } catch (e: Exception) {
                                isSuccess = false
                                resultMessage = "导出失败: ${e.message}"
                            }
                            isExporting = false
                            showResultDialog = true
                        }
                    },
                    enabled = exportBills || exportProjectBills || exportTodos || exportNotes || exportBirthdays
                ) {
                    Text("导出")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showImportSelectionDialog && selectedFileUri != null) {
        AlertDialog(
            onDismissRequest = {
                showImportSelectionDialog = false
                selectedFileUri = null
                selectedFileName = ""
            },
            title = { Text("选择导入内容") },
            text = {
                Column {
                    Text(
                        "已选择文件：$selectedFileName",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        "请选择要导入的数据类型：",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = importBills, onCheckedChange = { importBills = it })
                        Text("时间账单")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = importProjectBills, onCheckedChange = { importProjectBills = it })
                        Text("项目账单")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = importTodos, onCheckedChange = { importTodos = it })
                        Text("待办事项")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = importNotes, onCheckedChange = { importNotes = it })
                        Text("笔记")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = importBirthdays, onCheckedChange = { importBirthdays = it })
                        Text("生日管家")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showImportSelectionDialog = false
                        isImporting = true
                        val uri = selectedFileUri
                        val fileName = selectedFileName
                        selectedFileUri = null
                        selectedFileName = ""
                        coroutineScope.launch {
                            try {
                                val result = performImport(
                                    context,
                                    uri,
                                    fileName,
                                    importBills,
                                    importProjectBills,
                                    importTodos,
                                    importNotes,
                                    importBirthdays
                                )
                                isSuccess = true
                                resultMessage = result
                            } catch (e: Exception) {
                                isSuccess = false
                                resultMessage = "导入失败: ${e.message}"
                            }
                            isImporting = false
                            showResultDialog = true
                        }
                    },
                    enabled = (importBills || importProjectBills || importTodos || importNotes || importBirthdays)
                ) {
                    Text("导入")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showImportSelectionDialog = false
                        selectedFileUri = null
                        selectedFileName = ""
                    }
                ) {
                    Text("取消")
                }
            }
        )
    }

    if (showResultDialog) {
        AlertDialog(
            onDismissRequest = { showResultDialog = false },
            title = { Text(if (isSuccess) "操作成功" else "操作失败") },
            text = { Text(resultMessage) },
            confirmButton = {
                Button(onClick = { showResultDialog = false }) {
                    Text("确定")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("数据管理") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "数据备份",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "将您的数据导出为 JSON 文件，保存在 Downloads 文件夹中。您可以选择性备份部分或全部数据。",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showExportDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isExporting && !isImporting
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("导出中...")
                        } else {
                            Icon(Icons.Default.CloudUpload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("导出数据")
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "数据导入",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "从备份文件恢复数据。请先选择要导入的备份文件（JSON 格式）。",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = {
                            filePickerLauncher.launch(arrayOf("application/json", "*/*"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isExporting && !isImporting
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("选择备份文件")
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "使用说明",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "【导出】\n" +
                        "1. 点击「导出数据」\n" +
                        "2. 选择要导出的数据类型\n" +
                        "3. 数据将保存到 Downloads 文件夹\n" +
                        "4. 文件名格式：shijian_backup_YYYY-MM-DD.json\n\n" +
                        "【导入】\n" +
                        "1. 点击「选择备份文件」\n" +
                        "2. 在文件选择器中找到备份的 JSON 文件\n" +
                        "3. 选择要导入的数据类型\n" +
                        "4. 点击「导入」完成恢复\n\n" +
                        "注意：导入操作不会删除现有数据，仅进行合并",
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.3f
                    )
                }
            }
        }
    }
}

private suspend fun performExport(
    context: Context,
    exportBills: Boolean,
    exportProjectBills: Boolean,
    exportTodos: Boolean,
    exportNotes: Boolean,
    exportBirthdays: Boolean
): String = withContext(Dispatchers.IO) {
    val billsRepository = BillRepository(context)
    val todosRepository = TodoRepository(context)
    val notesRepository = NoteRepository(context)
    val birthdayRepository = BirthdayRepository(context)

    val allBills = billsRepository.getAllBills().first()
    val timeBills = allBills.filterIsInstance<TimeBill>()
    val projectBills = allBills.filterIsInstance<ProjectBill>()

    val filteredTimeBills = if (exportBills) timeBills else emptyList()
    val filteredProjectBills = if (exportProjectBills) projectBills else emptyList()
    val todos = if (exportTodos) todosRepository.getAllTodos().first().filterIsInstance<Todo>() else emptyList()
    val notes = if (exportNotes) notesRepository.getAllNotes().first().filterIsInstance<Note>() else emptyList()
    val birthdays = if (exportBirthdays) birthdayRepository.getAllBirthdays().first().filterIsInstance<Birthday>() else emptyList()

    val backupData = BackupData(
        bills = filteredTimeBills,
        projectBills = filteredProjectBills,
        todos = todos,
        notes = notes,
        birthdays = birthdays,
        backupDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    )

    val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }
    val jsonString = json.encodeToString(backupData)

    val fileName = "shijian_backup_${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}.json"

    val filePath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/json")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { outputStream ->
                outputStream.write(jsonString.toByteArray())
            }
        }
        "Downloads/$fileName"
    } else {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)
        FileOutputStream(file).use { outputStream ->
            outputStream.write(jsonString.toByteArray())
        }
        file.absolutePath
    }

    "数据已成功导出到\n$filePath"
}

private suspend fun performImport(
    context: Context,
    fileUri: Uri?,
    fileName: String,
    importBills: Boolean,
    importProjectBills: Boolean,
    importTodos: Boolean,
    importNotes: Boolean,
    importBirthdays: Boolean
): String = withContext(Dispatchers.IO) {
    if (fileUri == null) {
        throw Exception("未选择文件")
    }

    val billsRepository = BillRepository(context)
    val todosRepository = TodoRepository(context)
    val notesRepository = NoteRepository(context)
    val birthdayRepository = BirthdayRepository(context)

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    val jsonString = context.contentResolver.openInputStream(fileUri)?.bufferedReader()?.use { reader ->
        reader.readText()
    } ?: throw Exception("无法读取文件")

    val backupData = json.decodeFromString<BackupData>(jsonString)

    var importedCount = 0

    if (importBills && backupData.bills.isNotEmpty()) {
        val existingBills = billsRepository.getAllBills().first()
        val existingTimeBills = existingBills.filterIsInstance<TimeBill>()
        val newTimeBills = backupData.bills.filter { bill ->
            !existingTimeBills.any { it.identifier == bill.identifier }
        }
        billsRepository.saveBills(existingBills + newTimeBills)
        importedCount += newTimeBills.size
    }

    if (importProjectBills && backupData.projectBills.isNotEmpty()) {
        val existingBills = billsRepository.getAllBills().first()
        val existingProjectBills = existingBills.filterIsInstance<ProjectBill>()
        val newProjectBills = backupData.projectBills.filter { bill ->
            !existingProjectBills.any { it.identifier == bill.identifier }
        }
        billsRepository.saveBills(existingBills + newProjectBills)
        importedCount += newProjectBills.size
    }

    if (importTodos && backupData.todos.isNotEmpty()) {
        val existingTodos = todosRepository.getAllTodos().first()
        val newTodos = backupData.todos.filter { todo ->
            !existingTodos.any { it.identifier == todo.identifier }
        }
        todosRepository.saveTodos(existingTodos + newTodos)
        importedCount += newTodos.size
    }

    if (importNotes && backupData.notes.isNotEmpty()) {
        val existingNotes = notesRepository.getAllNotes().first()
        val newNotes = backupData.notes.filter { note ->
            !existingNotes.any { it.identifier == note.identifier }
        }
        notesRepository.saveNotes(existingNotes + newNotes)
        importedCount += newNotes.size
    }

    if (importBirthdays && backupData.birthdays.isNotEmpty()) {
        val existingBirthdays = birthdayRepository.getAllBirthdays().first()
        val newBirthdays = backupData.birthdays.filter { birthday ->
            !existingBirthdays.any { it.identifier == birthday.identifier }
        }
        birthdayRepository.saveBirthdays(existingBirthdays + newBirthdays)
        importedCount += newBirthdays.size
    }

    "成功导入 $importedCount 条数据"
}