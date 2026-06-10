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

private val Context.birthdaysDataStore by preferencesDataStore("birthdays")

class BirthdayRepository(private val context: Context) {
    private val BIRTHDAYS_KEY = stringPreferencesKey("birthdays")
    private val TAG = "BirthdayRepository"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun saveBirthdays(birthdays: List<Birthday>) {
        try {
            context.birthdaysDataStore.edit {
                val birthdaysJson = json.encodeToString(birthdays)
                Log.d(TAG, "Saving birthdays: $birthdaysJson")
                it[BIRTHDAYS_KEY] = birthdaysJson
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving birthdays", e)
        }
    }

    fun getAllBirthdays(): Flow<List<Birthday>> {
        return context.birthdaysDataStore.data.map {
            val birthdaysJson = it[BIRTHDAYS_KEY]
            if (birthdaysJson != null) {
                try {
                    json.decodeFromString<List<Birthday>>(birthdaysJson)
                } catch (e: Exception) {
                    Log.e(TAG, "Error decoding birthdays", e)
                    emptyList()
                }
            } else {
                emptyList()
            }
        }
    }

    suspend fun addBirthday(birthday: Birthday) {
        try {
            val currentBirthdays = getAllBirthdays().first()
            saveBirthdays(currentBirthdays + birthday)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding birthday", e)
        }
    }

    suspend fun deleteBirthday(birthday: Birthday) {
        try {
            val currentBirthdays = getAllBirthdays().first()
            saveBirthdays(currentBirthdays.filter { it.identifier != birthday.identifier })
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting birthday", e)
        }
    }

    suspend fun updateBirthday(updatedBirthday: Birthday) {
        try {
            val currentBirthdays = getAllBirthdays().first()
            saveBirthdays(currentBirthdays.map { b ->
                if (b.identifier == updatedBirthday.identifier) updatedBirthday else b
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error updating birthday", e)
        }
    }
}