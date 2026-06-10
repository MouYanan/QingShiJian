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
     * 发送待办事项通知（根据自定义提醒时间）
     */
    fun sendTodoNotification(todo: Todo) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            TODO_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val totalMinutes = todo.reminderHours * 60 + todo.reminderMinutes
        val timeText = if (todo.reminderHours > 0 && todo.reminderMinutes > 0) {
            "${todo.reminderHours}小时${todo.reminderMinutes}分钟"
        } else if (todo.reminderHours > 0) {
            "${todo.reminderHours}小时"
        } else if (todo.reminderMinutes > 0) {
            "${todo.reminderMinutes}分钟"
        } else {
            "4小时"
        }
        
        val notification = NotificationCompat.Builder(context, TODO_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("待办事项提醒")
            .setContentText("您的工作『${todo.title}』距离截止还剩${timeText}")
            .setStyle(NotificationCompat.BigTextStyle().bigText("您的工作『${todo.title}』距离截止还剩${timeText}"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()
        
        with(NotificationManagerCompat.from(context)) {
            notify(TODO_NOTIFICATION_ID + todo.identifier.hashCode(), notification)
        }
    }
    
    /**
     * 发送生日通知
     */
    fun sendBirthdayNotification(birthday: Birthday) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

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
        
        with(NotificationManagerCompat.from(context)) {
            notify(BIRTHDAY_NOTIFICATION_ID + birthday.identifier.hashCode(), notification)
        }
    }
    
    /**
     * 检查待办事项是否需要发送通知（根据自定义提醒时间）
     */
    fun checkTodoForNotification(todo: Todo): Boolean {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val dueDate = format.parse(todo.dueDate) ?: return false
            val now = Calendar.getInstance().time
            
            val remainingMs = dueDate.time - now.time
            val remainingMinutes = remainingMs / (1000 * 60)
            
            // 计算自定义提醒时间（分钟）
            val reminderMinutes = todo.reminderHours * 60 + todo.reminderMinutes
            val defaultReminderMinutes = 4 * 60 // 默认4小时
            val actualReminderMinutes = if (reminderMinutes > 0) reminderMinutes else defaultReminderMinutes
            
            // 检查是否在提醒时间范围内（±5分钟）
            remainingMinutes in (actualReminderMinutes - 5)..(actualReminderMinutes + 5) && remainingMs > 0
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check todo notification for: ${todo.title}", e)
            false
        }
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