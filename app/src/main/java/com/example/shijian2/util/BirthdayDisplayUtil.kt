package com.example.shijian2.util

import com.example.shijian2.data.Birthday
import java.text.SimpleDateFormat
import java.util.*

object BirthdayDisplayUtil {

    private val utc: TimeZone = TimeZone.getTimeZone("UTC")
    private val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = utc }
    private val outputFormat = SimpleDateFormat("yyyy年MM月dd日", Locale.US).apply { timeZone = utc }
    
    /**
     * 根据生日类型格式化显示日期
     */
    fun formatBirthdayForDisplay(birthday: Birthday): String {
        return when (birthday.calendarType) {
            "lunar" -> formatLunarBirthday(birthday.date)
            else -> formatSolarBirthday(birthday.date)
        }
    }
    
    /**
     * 格式化公历生日显示
     */
    private fun formatSolarBirthday(dateString: String): String {
        return try {
            val date = inputFormat.parse(dateString) ?: return "公历: $dateString"
            outputFormat.format(date)
        } catch (e: Exception) {
            "公历: $dateString"
        }
    }
    
    /**
     * 格式化农历生日显示
     */
    private fun formatLunarBirthday(dateString: String): String {
        return try {
            val lunarDate = LunarCalendarUtil.solarToLunar(dateString)
            val lunarDateStr = LunarCalendarUtil.getLunarDateString(lunarDate.first, lunarDate.second, lunarDate.third)
            "农历: ${lunarDate.first}年${lunarDateStr}"
        } catch (e: Exception) {
            "农历: $dateString"
        }
    }
    
    /**
     * 获取生日类型显示文本
     */
    fun getCalendarTypeDisplay(birthday: Birthday): String {
        return when (birthday.calendarType) {
            "lunar" -> "农历生日"
            else -> "公历生日"
        }
    }
    
    /**
     * 计算距离生日的天数（考虑农历/公历）
     */
    fun getDaysUntilBirthday(birthday: Birthday): Int {
        return when (birthday.calendarType) {
            "lunar" -> LunarCalendarUtil.getNextBirthdayDays(birthday.date, true)
            else -> LunarCalendarUtil.getNextBirthdayDays(birthday.date, false)
        }
    }
}