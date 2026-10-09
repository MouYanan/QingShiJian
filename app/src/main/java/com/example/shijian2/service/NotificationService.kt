package com.example.shijian2.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.shijian2.MainActivity
import com.example.shijian2.R
import com.example.shijian2.data.Birthday
import com.example.shijian2.data.Todo
import com.example.shijian2.util.BirthdayDisplayUtil
import com.example.shijian2.util.LunarCalendarUtil
import java.text.SimpleDateFormat
import java.util.*

class NotificationService(private val context: Context) {

    companion object {
        const val TODO_CHANNEL_ID = "todo_notifications"
        const val BIRTHDAY_CHANNEL_ID = "birthday_notifications"
        const val TODO_NOTIFICATION_ID = 1001
        const val BIRTHDAY_NOTIFICATION_ID = 1002

        /** 未自定义提醒时间时，默认提前 4 小时提醒 */
        private const val DEFAULT_REMINDER_MINUTES = 4 * 60

        /** 通知 id 命名空间：待办与生日分开，避免两者 id 落到同一区间互相覆盖 */
        private const val TODO_ID_NAMESPACE = 10
        private const val BIRTHDAY_ID_NAMESPACE = 20

        private const val TAG = "NotificationService"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 待办事项通知渠道
            val todoChannel = NotificationChannel(
                TODO_CHANNEL_ID,
                "待办事项提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "待办事项截止提醒"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }

            // 生日通知渠道
            val birthdayChannel = NotificationChannel(
                BIRTHDAY_CHANNEL_ID,
                "生日提醒",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "生日提醒通知"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(todoChannel)
            notificationManager.createNotificationChannel(birthdayChannel)
        }
    }

    /**
     * 判断某个渠道当前是否真的能弹出通知。
     *
     * 必须同时满足「应用级开关开启」与「该渠道未被单独关闭」。
     * 旧实现只判断 areNotificationsEnabled()，会漏掉用户在系统里单独关闭某个渠道的情况——
     * 那时该方法仍返回 true，而 notify() 会被系统静默丢弃，既不报错也无日志。
     */
    fun isChannelEnabled(channelId: String): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = manager.getNotificationChannel(channelId) ?: return true
            if (channel.importance == NotificationManager.IMPORTANCE_NONE) return false
        }
        return true
    }

    /** 解析待办截止时间，返回剩余毫秒；解析失败或已过期返回 null */
    private fun remainingMillisUntilDue(todo: Todo): Long? {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val dueDate = format.parse(todo.dueDate) ?: return null
            val remaining = dueDate.time - System.currentTimeMillis()
            if (remaining > 0) remaining else null
        } catch (e: Exception) {
            Log.w(TAG, "解析待办截止时间失败：${todo.title}", e)
            null
        }
    }

    /** 取用户配置的提前提醒量（分钟），未配置则用默认 4 小时 */
    private fun configuredReminderMinutes(todo: Todo): Int {
        val minutes = todo.reminderHours * 60 + todo.reminderMinutes
        return if (minutes > 0) minutes else DEFAULT_REMINDER_MINUTES
    }

    /** 把剩余毫秒格式化为「X小时Y分钟」的可读文案 */
    private fun formatRemaining(millis: Long): String {
        val totalMinutes = (millis + 59_999L) / 60_000L
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}小时${minutes}分钟"
            hours > 0 -> "${hours}小时"
            minutes > 0 -> "${minutes}分钟"
            else -> "不到1分钟"
        }
    }

    /** 生成稳定的非负通知 id，namespace 用于隔离不同业务类型 */
    private fun notificationId(namespace: Int, identifier: String): Int =
        namespace * 1_000_000 + (identifier.hashCode() and 0xFFFFF)

    /**
     * 发送待办提醒通知。
     *
     * @return true 表示已交给系统展示；false 表示被权限或渠道开关拦截。
     */
    fun sendTodoNotification(todo: Todo): Boolean {
        if (!isChannelEnabled(TODO_CHANNEL_ID)) {
            Log.w(TAG, "待办通知渠道不可用（权限被拒或渠道被关闭），跳过发送：${todo.title}")
            return false
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            TODO_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 用「实际剩余时间」而非配置值生成文案：调度若被 Doze 延迟，文案依然准确
        val remaining = remainingMillisUntilDue(todo)
        val contentText = if (remaining != null) {
            "您的工作『${todo.title}』距离截止还剩${formatRemaining(remaining)}"
        } else {
            "您的工作『${todo.title}』即将截止"
        }

        val notification = NotificationCompat.Builder(context, TODO_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("待办事项提醒")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        return try {
            NotificationManagerCompat.from(context)
                .notify(notificationId(TODO_ID_NAMESPACE, todo.identifier), notification)
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "发送待办通知被拒绝（缺少通知权限）", e)
            false
        }
    }

    /**
     * 发送生日提醒通知。
     *
     * @return true 表示已交给系统展示；false 表示被权限或渠道开关拦截。
     */
    fun sendBirthdayNotification(birthday: Birthday): Boolean {
        if (!isChannelEnabled(BIRTHDAY_CHANNEL_ID)) {
            Log.w(TAG, "生日通知渠道不可用（权限被拒或渠道被关闭），跳过发送：${birthday.name}")
            return false
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            BIRTHDAY_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendarTypeText = if (birthday.calendarType == "lunar") "农历" else "公历"
        val displayDate = BirthdayDisplayUtil.formatBirthdayForDisplay(birthday)

        val notification = NotificationCompat.Builder(context, BIRTHDAY_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("生日提醒")
            .setContentText("今天是${birthday.name}的${calendarTypeText}生日，不要忘记哦")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "今天是${birthday.name}的${calendarTypeText}生日，不要忘记哦\n$displayDate"
            ))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .build()

        return try {
            NotificationManagerCompat.from(context)
                .notify(notificationId(BIRTHDAY_ID_NAMESPACE, birthday.identifier), notification)
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "发送生日通知被拒绝（缺少通知权限）", e)
            false
        }
    }

    /**
     * 判断待办是否应触发提醒。
     *
     * 旧实现只在「剩余时间恰好落在提醒点 ±5 分钟」时返回 true。轮询周期是 15 分钟、
     * 还会被 Doze 延迟数十分钟，实际极易整段错过该窄窗口，而且不会有任何提示。
     *
     * 现改为：只要「已进入提醒窗口（剩余时间 <= 提醒提前量）且尚未过期」即返回 true。
     * 重复问题交给上层「每条待办每天只提醒一次」的去重保证，因此放宽窗口不会造成重复通知，
     * 却能稳定吸收调度抖动，避免漏发。
     */
    fun checkTodoForNotification(todo: Todo): Boolean {
        val remainingMs = remainingMillisUntilDue(todo) ?: return false
        val reminderMs = configuredReminderMinutes(todo) * 60_000L
        return remainingMs <= reminderMs
    }

    /**
     * 检查生日是否需要发送通知（支持农历和公历）
     */
    fun checkBirthdayForNotification(birthday: Birthday): Boolean {
        return try {
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (birthday.calendarType == "lunar") {
                // 农历生日：将农历日期转换为今年的公历日期再比较
                val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val birthdayDate = format.parse(birthday.date) ?: return false

                val lunarDate = LunarCalendarUtil.solarToLunar(birthday.date)
                val currentYear = today.get(Calendar.YEAR)

                // 将农历日期转换为今年的公历日期
                val solarThisYear = LunarCalendarUtil.lunarToSolar(
                    currentYear, lunarDate.second, lunarDate.third
                )

                if (solarThisYear.isNotEmpty()) {
                    val thisYearBirthday = format.parse(solarThisYear) ?: return false
                    val birthdayCalendar = Calendar.getInstance().apply {
                        time = thisYearBirthday
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    today.timeInMillis == birthdayCalendar.timeInMillis
                } else {
                    false
                }
            } else {
                // 公历生日：直接比较月日
                val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val birthdayDate = format.parse(birthday.date) ?: return false

                val birthdayCalendar = Calendar.getInstance().apply {
                    time = birthdayDate
                    set(Calendar.YEAR, today.get(Calendar.YEAR))
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                today.timeInMillis == birthdayCalendar.timeInMillis
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check birthday notification for: ${birthday.name}", e)
            false
        }
    }
}
