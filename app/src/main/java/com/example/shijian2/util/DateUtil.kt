package com.example.shijian2.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtil {
    
    /**
     * 格式化日期用于显示
     */
    fun formatDateForDisplay(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date)
        } catch (e: Exception) {
            dateString
        }
    }
    
    /**
     * 格式化日期时间用于显示
     */
    fun formatDateTimeForDisplay(dateTimeString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val outputFormat = SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault())
            val date = inputFormat.parse(dateTimeString)
            outputFormat.format(date)
        } catch (e: Exception) {
            dateTimeString
        }
    }
    
    /**
     * 计算距离生日的天数
     */
    fun getDaysUntilBirthday(birthdayString: String): Int {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val birthday = format.parse(birthdayString)
            val today = Calendar.getInstance()
            val birthdayCalendar = Calendar.getInstance()
            birthdayCalendar.time = birthday
            
            // 设置生日为今年
            birthdayCalendar.set(Calendar.YEAR, today.get(Calendar.YEAR))
            
            // 如果今年的生日已经过去，计算明年的生日
            if (birthdayCalendar.before(today)) {
                birthdayCalendar.add(Calendar.YEAR, 1)
            }
            
            val diff = birthdayCalendar.timeInMillis - today.timeInMillis
            (diff / (24 * 60 * 60 * 1000)).toInt()
        } catch (e: Exception) {
            -1
        }
    }
    
    /**
     * 获取剩余时间字符串
     */
    fun getRemainingTimeString(dueDateTimeString: String): String {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val dueDate = format.parse(dueDateTimeString)
            val now = Calendar.getInstance()
            val dueCalendar = Calendar.getInstance()
            dueCalendar.time = dueDate
            
            val diff = dueCalendar.timeInMillis - now.timeInMillis
            
            if (diff <= 0) {
                return "已过期"
            }
            
            val days = diff / (24 * 60 * 60 * 1000)
            val hours = (diff % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)
            val minutes = (diff % (60 * 60 * 1000)) / (60 * 1000)
            
            if (days > 0) {
                "剩余${days}天${hours}小时"
            } else if (hours > 0) {
                "剩余${hours}小时${minutes}分钟"
            } else {
                "剩余${minutes}分钟"
            }
        } catch (e: Exception) {
            "时间格式错误"
        }
    }
}