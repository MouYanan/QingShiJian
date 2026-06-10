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

private val Context.todosDataStore by preferencesDataStore("todos")

class TodoRepository(private val context: Context) {
    private val TODOS_KEY = stringPreferencesKey("todos")
    private val TAG = "TodoRepository"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun saveTodos(todos: List<Todo>) {
        try {
            context.todosDataStore.edit {
                val todosJson = json.encodeToString(todos)
                Log.d(TAG, "Saving todos: $todosJson")
                it[TODOS_KEY] = todosJson
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving todos", e)
        }
    }

    fun getAllTodos(): Flow<List<Todo>> {
        return context.todosDataStore.data.map {
            val todosJson = it[TODOS_KEY]
            if (todosJson != null) {
                try {
                    json.decodeFromString<List<Todo>>(todosJson)
                } catch (e: Exception) {
                    Log.e(TAG, "Error decoding todos", e)
                    emptyList()
                }
            } else {
                emptyList()
            }
        }
    }

    suspend fun addTodo(todo: Todo) {
        try {
            val currentTodos = getAllTodos().first()
            saveTodos(currentTodos + todo)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding todo", e)
        }
    }

    suspend fun deleteTodo(todo: Todo) {
        try {
            val currentTodos = getAllTodos().first()
            saveTodos(currentTodos.filter { it.identifier != todo.identifier })
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting todo", e)
        }
    }

    suspend fun updateTodo(updatedTodo: Todo) {
        try {
            val currentTodos = getAllTodos().first()
            saveTodos(currentTodos.map { t ->
                if (t.identifier == updatedTodo.identifier) updatedTodo else t
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error updating todo", e)
        }
    }
}