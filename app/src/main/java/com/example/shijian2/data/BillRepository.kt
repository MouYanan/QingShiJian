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

private val Context.billsDataStore by preferencesDataStore("bills")

class BillRepository(private val context: Context) {
    private val BILLS_KEY = stringPreferencesKey("bills")
    private val TAG = "BillRepository"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        classDiscriminator = "type"
    }

    suspend fun saveBills(bills: List<Bill>) {
        try {
            context.billsDataStore.edit {
                val billsJson = json.encodeToString(bills)
                Log.d(TAG, "Saving bills: $billsJson")
                it[BILLS_KEY] = billsJson
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving bills", e)
        }
    }

    fun getAllBills(): Flow<List<Bill>> {
        return context.billsDataStore.data.map {
            val billsJson = it[BILLS_KEY]
            Log.d(TAG, "Reading bills from DataStore: $billsJson")
            if (billsJson != null) {
                try {
                    json.decodeFromString<List<Bill>>(billsJson)
                } catch (e: Exception) {
                    Log.e(TAG, "Error decoding bills", e)
                    emptyList()
                }
            } else {
                Log.d(TAG, "No bills found in DataStore")
                emptyList()
            }
        }
    }

    suspend fun addBill(bill: Bill) {
        try {
            Log.d(TAG, "Adding bill: $bill")
            val currentBills = getAllBills().first()
            Log.d(TAG, "Current bills count: ${currentBills.size}")
            val newBills = currentBills + bill
            Log.d(TAG, "New bills count: ${newBills.size}")
            saveBills(newBills)
            Log.d(TAG, "Bill added successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding bill", e)
        }
    }

    suspend fun deleteBill(bill: Bill) {
        try {
            val currentBills = getAllBills().first()
            saveBills(currentBills.filter { it.identifier != bill.identifier })
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting bill", e)
        }
    }

    suspend fun updateBill(updatedBill: Bill) {
        try {
            val currentBills = getAllBills().first()
            val newBills = currentBills.map { bill ->
                if (bill.identifier == updatedBill.identifier) {
                    updatedBill
                } else {
                    bill
                }
            }
            saveBills(newBills)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating bill", e)
        }
    }
}