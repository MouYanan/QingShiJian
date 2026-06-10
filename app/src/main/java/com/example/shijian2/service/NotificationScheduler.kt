package com.example.shijian2.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.work.*
import com.example.shijian2.data.BirthdayRepository
import com.example.shijian2.data.TodoRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.*
import java.util.concurrent.TimeUnit

class NotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            checkNotifications()
            Result.success()
        } catch (e: Exception) {
            Log.e("NotificationWorker", "Error checking notifications", e)
            Result.failure()
        }
    }

    private suspend fun checkNotifications() {
        val context = applicationContext
        val notificationService = NotificationService(context)
        val prefs = context.getSharedPreferences("notification_sent", Context.MODE_PRIVATE)

        // 检查通知开关
        val settingsRepo = com.example.shijian2.data.SettingsRepository(context)
        val notificationsEnabled = settingsRepo.getNotifications()
        if (!notificationsEnabled) {
            Log.d("NotificationWorker", "Notifications disabled, skipping check")
            return
        }

        // 清理过期的已发送记录（每天清理一次）
        cleanOldSentRecords(prefs)

        // 检查待办事项（仅检查未完成的）
        val todoRepository = TodoRepository(context)
        val todos = todoRepository.getAllTodos().first()
        todos.filter { it.status != "completed" }.forEach { todo ->
            if (notificationService.checkTodoForNotification(todo)) {
                val sentKey = "todo_${todo.identifier}_${getDateKey()}"
                if (!prefs.getBoolean(sentKey, false)) {
                    notificationService.sendTodoNotification(todo)
                    prefs.edit().putBoolean(sentKey, true).apply()
                    Log.d("NotificationWorker", "Sent todo notification: ${todo.title}")
                }
            }
        }

        // 检查生日
        val birthdayRepository = BirthdayRepository(context)
        val birthdays = birthdayRepository.getAllBirthdays().first()
        birthdays.forEach { birthday ->
            if (notificationService.checkBirthdayForNotification(birthday)) {
                val sentKey = "birthday_${birthday.identifier}_${getDateKey()}"
                if (!prefs.getBoolean(sentKey, false)) {
                    notificationService.sendBirthdayNotification(birthday)
                    prefs.edit().putBoolean(sentKey, true).apply()
                    Log.d("NotificationWorker", "Sent birthday notification: ${birthday.name}")
                }
            }
        }
    }

    private fun getDateKey(): String {
        val cal = Calendar.getInstance()
        return String.format("%d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
    }

    private fun cleanOldSentRecords(prefs: SharedPreferences) {
        val todayKey = getDateKey()
        val keys = prefs.all.keys.toList()
        val edit = prefs.edit()
        keys.forEach { key ->
            // 保留今天的记录，删除其他
            if (!key.endsWith(todayKey)) {
                edit.remove(key)
            }
        }
        edit.apply()
    }
}

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 启动通知检查工作
        NotificationScheduler.scheduleImmediateNotificationCheck(context)
    }
}

object NotificationScheduler {
    
    fun scheduleDailyNotificationCheck(context: Context) {
        // 取消之前的任务
        cancelAllNotifications(context)
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresCharging(false)
            .setRequiresBatteryNotLow(false)
            .build()
        
        // 每天检查一次（用于生日提醒，最小间隔15分钟）
        val dailyCheck = PeriodicWorkRequestBuilder<NotificationWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setInitialDelay(1, TimeUnit.MINUTES)
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "daily_notification_check",
            ExistingPeriodicWorkPolicy.REPLACE,
            dailyCheck
        )
        
        Log.d("NotificationScheduler", "Notification check scheduled")
    }
    
    fun scheduleImmediateNotificationCheck(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresCharging(false)
            .build()
        
        val immediateCheck = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setConstraints(constraints)
            .setInitialDelay(5, TimeUnit.SECONDS)
            .build()
        
        WorkManager.getInstance(context).enqueue(immediateCheck)
    }
    
    fun cancelAllNotifications(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork("daily_notification_check")
        WorkManager.getInstance(context).cancelUniqueWork("frequent_notification_check")
        Log.d("NotificationScheduler", "All notification checks cancelled")
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            // 设备重启后通过WorkManager异步检查通知开关并重新调度
            val workRequest = OneTimeWorkRequestBuilder<BootCheckWorker>()
                .setInitialDelay(5, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}

class BootCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val settingsRepo = com.example.shijian2.data.SettingsRepository(applicationContext)
        val enabled = settingsRepo.getNotifications()
        if (enabled) {
            NotificationScheduler.scheduleDailyNotificationCheck(applicationContext)
        }
        return Result.success()
    }
}