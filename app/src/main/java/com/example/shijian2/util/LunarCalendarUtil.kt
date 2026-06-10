package com.example.shijian2.util

import java.text.SimpleDateFormat
import java.util.*

object LunarCalendarUtil {
    private val chineseDays = arrayOf("初一","初二","初三","初四","初五","初六","初七","初八","初九","初十","十一","十二","十三","十四","十五","十六","十七","十八","十九","二十","廿一","廿二","廿三","廿四","廿五","廿六","廿七","廿八","廿九","三十")
    private val chineseMonths = arrayOf("正月","二月","三月","四月","五月","六月","七月","八月","九月","十月","冬月","腊月")

    // 使用 UTC 时区进行所有日期计算，避免时区和夏令时导致日期偏移
    private val utc: TimeZone = TimeZone.getTimeZone("UTC")
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = utc }

    private val lunarInfo = intArrayOf(
        0x04bd8, 0x04ae0, 0x0a570, 0x054d5, 0x0d260, 0x0d950, 0x16554, 0x056a0, 0x09ad0, 0x055d2,
        0x04ae0, 0x0a5b6, 0x0a4d0, 0x0d250, 0x1d255, 0x0b540, 0x0d6a0, 0x0ada2, 0x095b0, 0x14977,
        0x04970, 0x0a4b0, 0x0b4b5, 0x06a50, 0x06d40, 0x1ab54, 0x02b60, 0x09570, 0x052f2, 0x04970,
        0x06566, 0x0d4a0, 0x0ea50, 0x06e95, 0x05ad0, 0x02b60, 0x186e3, 0x092e0, 0x1c8d7, 0x0c950,
        0x0d4a0, 0x1d8a6, 0x0b550, 0x056a0, 0x1a5b4, 0x025d0, 0x092d0, 0x0d2b2, 0x0a950, 0x0b557,
        0x06ca0, 0x0b550, 0x15355, 0x04da0, 0x0a5b0, 0x14573, 0x052b0, 0x0a9a8, 0x0e950, 0x06aa0,
        0x0aea6, 0x0ab50, 0x04b60, 0x0aae4, 0x0a570, 0x05260, 0x0f263, 0x0d950, 0x05b57, 0x056a0,
        0x096d0, 0x04dd5, 0x04ad0, 0x0a4d0, 0x0d4d4, 0x0d250, 0x0d558, 0x0b540, 0x0b6a0, 0x195a6,
        0x095b0, 0x049b0, 0x0a974, 0x0a4b0, 0x0b27a, 0x06a50, 0x06d40, 0x0af46, 0x0ab60, 0x09570,
        0x04af5, 0x04970, 0x064b0, 0x074a3, 0x0ea50, 0x06b58, 0x055c0, 0x0ab60, 0x096d5, 0x092e0,
        0x0c960, 0x0d954, 0x0d4a0, 0x0da50, 0x07552, 0x056a0, 0x0abb7, 0x025d0, 0x092d0, 0x0cab5,
        0x0a950, 0x0b4a0, 0x0baa4, 0x0ad50, 0x055d9, 0x04ba0, 0x0a5b0, 0x15176, 0x052b0, 0x0a930,
        0x07954, 0x06aa0, 0x0ad50, 0x05b52, 0x04b60, 0x0a6e6, 0x0a4e0, 0x0d260, 0x0ea65, 0x0d530,
        0x05aa0, 0x076a3, 0x096d0, 0x04afb, 0x04ad0, 0x0a4d0, 0x1d0b6, 0x0d250, 0x0d520, 0x0dd45,
        0x0b5a0, 0x056d0, 0x055b2, 0x049b0, 0x0a577, 0x0a4b0, 0x0aa50, 0x1b255, 0x06d20, 0x0ada0,
        0x14b63, 0x09370, 0x049f8, 0x04970, 0x064b0, 0x168a6, 0x0ea50, 0x06b20, 0x1a6c4, 0x0aae0,
        0x0a2e0, 0x0d2e3, 0x0c960, 0x0d557, 0x0d4a0, 0x0da50, 0x05d55, 0x056a0, 0x0a6d0, 0x055d4,
        0x052d0, 0x0a9b8, 0x0e950, 0x06aa0, 0x056a0, 0x096d5, 0x04b60, 0x0a6e4, 0x0a4e4, 0x0d260,
        0x0e965, 0x0d530, 0x05aa0, 0x076a3, 0x096d0, 0x04afb, 0x04ad0, 0x0a4d0, 0x1d0b6, 0x0d250,
        0x0d520, 0x0dd45, 0x0b5a0, 0x056d0, 0x055b2, 0x049b0, 0x0a577, 0x0a4b0, 0x0aa50, 0x1b255,
        0x06d20, 0x0ada0, 0x14b63, 0x09370, 0x049f8, 0x04970, 0x064b0, 0x168a6, 0x0ea50, 0x06b20,
        0x1a6c4, 0x0aae0, 0x0a2e0, 0x0d2e3, 0x0c960, 0x0d557, 0x0d4a0, 0x0da50, 0x05d55, 0x056a0,
        0x0a6d0, 0x055d4, 0x052d0, 0x0a9b8, 0x0e950, 0x06aa0, 0x056a0, 0x096d5, 0x04b60, 0x0a6e4,
        0x0a4e4, 0x0d260, 0x0e965, 0x0d530, 0x05aa0, 0x076a3, 0x096d0, 0x04afb, 0x04ad0, 0x0a4d0,
        0x1d0b6, 0x0d250, 0x0d520, 0x0dd45, 0x0b5a0, 0x056d0, 0x055b2, 0x049b0, 0x0a577, 0x0a4b0,
        0x0aa50, 0x1b255, 0x06d20, 0x0ada0, 0x14b63, 0x09370, 0x049f8, 0x04970, 0x064b0, 0x168a6,
        0x0ea50, 0x06b20, 0x1a6c4, 0x0aae0, 0x0a2e0, 0x0d2e3, 0x0c960, 0x0d557, 0x0d4a0, 0x0da50,
        0x05d55, 0x056a0, 0x0a6d0, 0x055d4, 0x052d0, 0x0a9b8, 0x0e950, 0x06aa0, 0x056a0, 0x096d5,
        0x04b60, 0x0a6e4, 0x0a4e4, 0x0d260, 0x0e965, 0x0d530, 0x05aa0, 0x076a3, 0x096d0, 0x04afb,
        0x04ad0, 0x0a4d0, 0x1d0b6, 0x0d250, 0x0d520, 0x0dd45, 0x0b5a0, 0x056d0, 0x055b2, 0x049b0,
        0x0a577, 0x0a4b0, 0x0aa50, 0x1b255, 0x06d20, 0x0ada0, 0x14b63, 0x09370, 0x049f8, 0x04970,
        0x064b0, 0x168a6, 0x0ea50, 0x06b20, 0x1a6c4, 0x0aae0, 0x0a2e0, 0x0d2e3, 0x0c960, 0x0d557,
        0x0d4a0, 0x0da50, 0x05d55, 0x056a0, 0x0a6d0, 0x055d4, 0x052d0, 0x0a9b8, 0x0e950, 0x06aa0,
        0x056a0, 0x096d5, 0x04b60, 0x0a6e4, 0x0a4e4, 0x0d260, 0x0e965, 0x0d530, 0x05aa0, 0x076a3,
        0x096d0, 0x04afb, 0x04ad0, 0x0a4d0, 0x1d0b6, 0x0d250, 0x0d520, 0x0dd45, 0x0b5a0, 0x056d0,
        0x055b2, 0x049b0, 0x0a577, 0x0a4b0, 0x0aa50, 0x1b255, 0x06d20, 0x0ada0, 0x14b63, 0x09370,
        0x049f8, 0x04970, 0x064b0, 0x16aa6, 0x0ea50, 0x06b20, 0x1a6c4, 0x0aae0, 0x0a2e0, 0x0d2e3,
        0x0c960, 0x0d557, 0x0d4a0, 0x0da50, 0x05d55, 0x056a0, 0x0a6d0, 0x055d4, 0x052d0, 0x0a9b8,
        0x0e950, 0x06aa0, 0x056a0, 0x096d5, 0x04b60, 0x0a6e4, 0x0a4e4, 0x0d260, 0x0e965, 0x0d530,
        0x05aa0, 0x076a3, 0x096d0, 0x04afb, 0x04ad0, 0x0a4d0, 0x1d0b6, 0x0d250, 0x0d520, 0x0dd45,
        0x0b5a0, 0x056d0, 0x055b2, 0x049b0, 0x0a577, 0x0a4b0, 0x0aa50, 0x1b255, 0x06d20, 0x0ada0,
        0x14b63, 0x09370, 0x049f8, 0x04970, 0x064b0, 0x16aa6, 0x0ea50, 0x06b20, 0x1a6c4, 0x0aae0,
        0x0a2e0, 0x0d2e3, 0x0c960, 0x0d557, 0x0d4a0, 0x0da50, 0x05d55, 0x056a0, 0x0a6d0, 0x055d4
    )

    // 基准日期：1900年1月31日（农历正月初一），使用 UTC 时区
    private val baseDate: Calendar = Calendar.getInstance(utc).apply {
        set(1900, Calendar.JANUARY, 31, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }

    fun lunarToSolar(lunarYear: Int, lunarMonth: Int, lunarDay: Int): String {
        val offset = calculateLunarOffset(lunarYear, lunarMonth, lunarDay)
        val solarCalendar = Calendar.getInstance(utc)
        solarCalendar.timeInMillis = baseDate.timeInMillis
        solarCalendar.add(Calendar.DAY_OF_YEAR, offset)
        return dateFormat.format(solarCalendar.time)
    }

    fun solarToLunar(solarDate: String): Triple<Int, Int, Int> {
        val date = dateFormat.parse(solarDate) ?: return Triple(0, 0, 0)

        val offset = daysBetween(baseDate.time, date)
        var lunarYear = 1900
        var daysAccumulated = 0

        for (i in 1900 until 2100) {
            val yearDays = totalDaysOfYear(i)
            if (offset < daysAccumulated + yearDays) {
                lunarYear = i
                break
            }
            daysAccumulated += yearDays
        }

        val offsetInYear = offset - daysAccumulated
        val leapMonth = getLeapMonth(lunarYear)
        var lunarMonth = 1
        var daysAccumulatedMonth = 0

        for (i in 1..12) {
            // 处理闰月（插入在 leapMonth 之后）
            if (leapMonth > 0 && i == leapMonth + 1) {
                val leapDays = getLeapDays(lunarYear)
                if (offsetInYear < daysAccumulatedMonth + leapDays) {
                    // 日期落在闰月（当前暂不区分闰月与普通月，使用 i 作为月份在后续显示时需注意）
                    lunarMonth = i
                    break
                }
                daysAccumulatedMonth += leapDays
            }

            val days = monthDays(lunarYear, i)
            if (offsetInYear < daysAccumulatedMonth + days) {
                lunarMonth = i
                break
            }
            daysAccumulatedMonth += days
        }

        val lunarDay = offsetInYear - daysAccumulatedMonth + 1

        return Triple(lunarYear, lunarMonth, lunarDay)
    }

    fun getLunarDateString(lunarYear: Int, lunarMonth: Int, lunarDay: Int): String {
        val monthStr = if (lunarMonth < 1 || lunarMonth > 12) "" else chineseMonths[lunarMonth - 1]
        val dayStr = if (lunarDay < 1 || lunarDay > 30) "" else chineseDays[lunarDay - 1]
        return "$monthStr$dayStr"
    }

    fun getNextBirthdayDays(birthdayDate: String, isLunar: Boolean): Int {
        val today = Calendar.getInstance(utc)
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        if (isLunar) {
            val lunar = solarToLunar(birthdayDate)
            val birthLunarMonth = lunar.second
            val birthLunarDay = lunar.third

            val todaySolarStr = dateFormat.format(today.time)
            val currentLunar = solarToLunar(todaySolarStr)
            val todayLunarMonth = currentLunar.second
            val todayLunarDay = currentLunar.third

            var nextLunarYear = today.get(Calendar.YEAR)

            val isBirthdayPassed = if (birthLunarMonth < todayLunarMonth) {
                true
            } else if (birthLunarMonth == todayLunarMonth) {
                birthLunarDay <= todayLunarDay
            } else {
                false
            }

            if (isBirthdayPassed) {
                nextLunarYear++
            }

            val nextSolarDate = lunarToSolar(nextLunarYear, birthLunarMonth, birthLunarDay)
            val nextDate = dateFormat.parse(nextSolarDate)
            return daysBetween(today.time, nextDate!!)
        } else {
            val birthdayParts = birthdayDate.split("-")
            val birthMonth = birthdayParts[1].toInt() - 1
            val birthDay = birthdayParts[2].toInt()

            val todayYear = today.get(Calendar.YEAR)

            val nextBirthday = Calendar.getInstance(utc)
            nextBirthday.set(todayYear, birthMonth, birthDay, 0, 0, 0)
            nextBirthday.set(Calendar.MILLISECOND, 0)

            if (nextBirthday.before(today) || isSameDay(nextBirthday, today)) {
                nextBirthday.set(Calendar.YEAR, todayYear + 1)
            }

            return daysBetween(today.time, nextBirthday.time)
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
    }

    private fun calculateLunarOffset(lunarYear: Int, lunarMonth: Int, lunarDay: Int): Int {
        var offset = 0
        for (i in 1900 until lunarYear) {
            offset += totalDaysOfYear(i)
        }
        val leapMonth = getLeapMonth(lunarYear)
        if (lunarMonth > leapMonth) {
            offset += getLeapDays(lunarYear)
        }
        for (i in 1 until lunarMonth) {
            offset += monthDays(lunarYear, i)
        }
        offset += lunarDay - 1
        return offset
    }

    private fun totalDaysOfYear(year: Int): Int {
        if (year < 1900 || year > 2100) return 0
        var sum = 348
        var i = 0x8000
        while (i > 0x8) {
            sum += if ((lunarInfo[year - 1900] and i) != 0) 1 else 0
            i = i shr 1
        }
        return sum + getLeapDays(year)
    }

    fun getLeapMonth(year: Int): Int {
        if (year < 1900 || year > 2100) return 0
        return (lunarInfo[year - 1900] and 0xf).toInt()
    }

    fun getLeapDays(year: Int): Int {
        if (year < 1900 || year > 2100) return 0
        return if (getLeapMonth(year) > 0) {
            if ((lunarInfo[year - 1900] and 0x10000) != 0) 30 else 29
        } else 0
    }

    fun monthDays(year: Int, month: Int): Int {
        if (year < 1900 || year > 2100) return 0
        return if ((lunarInfo[year - 1900] and (0x10000 shr month)) != 0) 30 else 29
    }

    private fun daysBetween(start: Date, end: Date): Int {
        // 使用 UTC 时区计算，避免夏令时导致天数偏差
        val startCal = Calendar.getInstance(utc).apply { time = start }
        val endCal = Calendar.getInstance(utc).apply { time = end }

        // 清除时分秒，只比较日期
        startCal.set(Calendar.HOUR_OF_DAY, 0)
        startCal.set(Calendar.MINUTE, 0)
        startCal.set(Calendar.SECOND, 0)
        startCal.set(Calendar.MILLISECOND, 0)
        endCal.set(Calendar.HOUR_OF_DAY, 0)
        endCal.set(Calendar.MINUTE, 0)
        endCal.set(Calendar.SECOND, 0)
        endCal.set(Calendar.MILLISECOND, 0)

        val diffMillis = endCal.timeInMillis - startCal.timeInMillis
        return (diffMillis / (1000 * 60 * 60 * 24)).toInt()
    }
}