package com.example.shijian2.ui.note

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
    noteToEdit: Note? = null
) {
    val context = LocalContext.current
    val repository = remember { NoteRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var title by remember { mutableStateOf(noteToEdit?.title ?: "") }
    var content by remember { mutableStateOf(noteToEdit?.content ?: "") }
    var isSaving by remember { mutableStateOf(false) }

    fun saveNote() {
        if (title.isEmpty()) return

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
            label = { Text("笔记标题 *") },
            singleLine = true,
            enabled = !isSaving
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            label = { Text("笔记内容") },
            enabled = !isSaving
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f),
                enabled = !isSaving
            ) {
                Text("取消")
            }
            Button(
                onClick = { saveNote() },
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
    }
}