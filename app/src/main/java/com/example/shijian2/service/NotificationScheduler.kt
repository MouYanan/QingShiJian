package com.example.shijian2.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.work.*
import com.example.shijian2.data.BirthdayRepository
import com.example.shijian2.data.TodoRepository
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
            // 关键：周期任务绝不能返回 failure。
            // 对 PeriodicWorkRequest 而言 failure 是终止态，会让整条提醒链路被永久停掉；
            // 这里改为 retry，交给 WorkManager 按退避策略重试。
            Log.e("NotificationWorker", "检查通知时出错，将在下个周期重试", e)
            Result.retry()
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

/**
 * 供应用内显式触发一次即时检查使用（不再对外导出，避免被第三方 App 直接调起）。
 */
class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NotificationScheduler.scheduleImmediateNotificationCheck(context)
    }
}

object NotificationScheduler {

    private const val PERIODIC_WORK_NAME = "daily_notification_check"

    /**
     * 确保周期检查任务已排入队列（幂等）。
     *
     * 使用 ExistingPeriodicWorkPolicy.KEEP：重复调用不会取消并重建任务，
     * 因此不会像 REPLACE 那样在每次打开 App 时把 15 分钟计时相位反复清零，
     * 也不会因为「刚打开就重排」而迟迟等不到第一次检查。
     * 需要真正重启任务时（例如用户重新打开通知开关），先调用 cancelAllNotificationChecks()。
     */
    fun schedulePeriodicNotificationCheck(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresCharging(false)
            .setRequiresBatteryNotLow(false)
            .build()

        // 15 分钟是 WorkManager 周期任务允许的最小间隔
        val periodicCheck = PeriodicWorkRequestBuilder<NotificationWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setInitialDelay(1, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicCheck
        )

        Log.d("NotificationScheduler", "Periodic notification check ensured (KEEP)")
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

    fun cancelAllNotificationChecks(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
        Log.d("NotificationScheduler", "Periodic notification check cancelled")
    }
}

/**
 * 开机广播接收器。
 *
 * 只处理 BOOT_COMPLETED / QUICKBOOT_POWERON：应用数据存放在凭据保护存储中，
 * 在用户解锁前（LOCKED_BOOT_COMPLETED 阶段）无法读取，故不再声明该 action。
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            // 设备重启后通过 WorkManager 异步检查通知开关并重新调度
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
            NotificationScheduler.schedulePeriodicNotificationCheck(applicationContext)
        }
        return Result.success()
    }
}
