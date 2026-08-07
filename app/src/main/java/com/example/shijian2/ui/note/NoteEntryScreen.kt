package com.example.shijian2.ui.note

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.shijian2.data.Note
import com.example.shijian2.data.NoteRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEntryScreen(
    onBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    noteToEdit: Note? = null,
    onSaveActionChange: ((() -> Unit)?) -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { NoteRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val textColor = MaterialTheme.colorScheme.onSurface

    var title by remember { mutableStateOf(noteToEdit?.title ?: "") }
    var content by remember { mutableStateOf(noteToEdit?.content ?: "") }
    var isSaving by remember { mutableStateOf(false) }

    fun saveNote() {
        if (title.isEmpty() || isSaving) return

        isSaving = true
        coroutineScope.launch {
            val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val note = if (noteToEdit != null) {
                noteToEdit.copy(
                    title = title,
                    content = content,
                    updatedAt = currentDate
                )
            } else {
                Note(
                    id = java.util.UUID.randomUUID().toString(),
                    title = title,
                    content = content,
                    createdAt = currentDate
                )
            }

            if (noteToEdit != null) {
                repository.updateNote(note)
            } else {
                repository.addNote(note)
            }
            isSaving = false
            onSaveSuccess()
        }
    }

    // 注册 / 注销保存回调，供顶部导航栏调用
    DisposableEffect(Unit) {
        onSaveActionChange { saveNote() }
        onDispose { onSaveActionChange(null) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(scrollState)
    ) {
        // 标题输入框（无边框）
        BasicTextField(
            value = title,
            onValueChange = { title = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isSaving,
            textStyle = MaterialTheme.typography.titleLarge.copy(color = textColor),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                Box {
                    if (title.isEmpty()) {
                        Text(
                            "标题",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    innerTextField()
                }
            }
        )

        // 标题与正文之间的分割线
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // 正文输入框（无边框，自适应高度）
        BasicTextField(
            value = content,
            onValueChange = { content = it },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
            enabled = !isSaving,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = textColor),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                Box {
                    if (content.isEmpty()) {
                        Text(
                            "正文",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}
