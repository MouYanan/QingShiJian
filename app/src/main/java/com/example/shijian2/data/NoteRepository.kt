package com.example.shijian2.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.notesDataStore by preferencesDataStore("notes")

class NoteRepository(private val context: Context) {
    private val NOTES_KEY = stringPreferencesKey("notes")
    private val TAG = "NoteRepository"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun saveNotes(notes: List<Note>) {
        try {
            context.notesDataStore.edit {
                val notesJson = json.encodeToString(notes)
                Log.d(TAG, "Saving notes: $notesJson")
                it[NOTES_KEY] = notesJson
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving notes", e)
        }
    }

    fun getAllNotes(): Flow<List<Note>> {
        return context.notesDataStore.data.map {
            val notesJson = it[NOTES_KEY]
            if (notesJson != null) {
                try {
                    json.decodeFromString<List<Note>>(notesJson)
                } catch (e: Exception) {
                    Log.e(TAG, "Error decoding notes", e)
                    emptyList()
                }
            } else {
                emptyList()
            }
        }
    }

    suspend fun addNote(note: Note) {
        try {
            val currentNotes = getAllNotes().first()
            saveNotes(currentNotes + note)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding note", e)
        }
    }

    suspend fun deleteNote(note: Note) {
        try {
            val currentNotes = getAllNotes().first()
            saveNotes(currentNotes.filter { it.identifier != note.identifier })
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting note", e)
        }
    }

    suspend fun updateNote(updatedNote: Note) {
        try {
            val currentNotes = getAllNotes().first()
            saveNotes(currentNotes.map { n ->
                if (n.identifier == updatedNote.identifier) updatedNote else n
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error updating note", e)
        }
    }
}