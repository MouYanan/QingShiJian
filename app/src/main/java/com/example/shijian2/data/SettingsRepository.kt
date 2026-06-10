package com.example.shijian2.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings"
)

class SettingsRepository(private val context: Context) {
    companion object {
        private val THEME_KEY = stringPreferencesKey("theme")
        private val NOTIFICATIONS_KEY = booleanPreferencesKey("notifications")
        private val BIRTHDAY_SORT_KEY = stringPreferencesKey("birthday_sort")
        private val NOTE_SORT_KEY = stringPreferencesKey("note_sort")
        private val TODO_SORT_KEY = stringPreferencesKey("todo_sort")
    }

    suspend fun getTheme(): String {
        val preferences = context.settingsDataStore.data.first()
        return preferences[THEME_KEY] ?: "light"
    }

    fun getThemeFlow(): Flow<String> {
        return context.settingsDataStore.data.map { preferences ->
            preferences[THEME_KEY] ?: "light"
        }
    }

    suspend fun setTheme(theme: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[THEME_KEY] = theme
        }
    }

    suspend fun getNotifications(): Boolean {
        val preferences = context.settingsDataStore.data.first()
        return preferences[NOTIFICATIONS_KEY] ?: true
    }

    suspend fun setNotifications(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[NOTIFICATIONS_KEY] = enabled
        }
    }

    // 生日排序模式持久化
    fun getBirthdaySortFlow(): Flow<String> {
        return context.settingsDataStore.data.map { preferences ->
            preferences[BIRTHDAY_SORT_KEY] ?: "BIRTHDAY_TIME_DESC"
        }
    }

    suspend fun setBirthdaySort(mode: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[BIRTHDAY_SORT_KEY] = mode
        }
    }

    // 笔记排序模式持久化
    fun getNoteSortFlow(): Flow<String> {
        return context.settingsDataStore.data.map { preferences ->
            preferences[NOTE_SORT_KEY] ?: "NOTE_PINYIN_ASC"
        }
    }

    suspend fun setNoteSort(mode: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[NOTE_SORT_KEY] = mode
        }
    }

    // 待办排序模式持久化
    fun getTodoSortFlow(): Flow<String> {
        return context.settingsDataStore.data.map { preferences ->
            preferences[TODO_SORT_KEY] ?: "TODO_PINYIN_ASC"
        }
    }

    suspend fun setTodoSort(mode: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[TODO_SORT_KEY] = mode
        }
    }
}