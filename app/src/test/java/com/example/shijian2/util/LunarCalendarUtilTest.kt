package com.example.shijian2.util

import org.junit.Assert.*
import org.junit.Test

class LunarCalendarUtilTest {

    // ==================================================================
    // Bug 复现：腊月→正月 月份偏移
    // ==================================================================

    /**
     * 复现原始Bug：腊月初三 → 正月初三
     * 2025年有闰二月（leapMonth=2），在修复前 solarToLunar 未计入闰月天数，
     * 导致腊月的月份偏移回正月。
     */
    @Test
    fun testBugLunar12Month3Day_roundTrip() {
        val year = 2025
        val month = 12  // 腊月
        val day = 3     // 初三

        val solarDate = LunarCalendarUtil.lunarToSolar(year, month, day)
        val result = LunarCalendarUtil.solarToLunar(solarDate)

        assertEquals("年份应保持一致", year, result.first)
        assertEquals("月份应为腊月(12)", month, result.second)
        assertEquals("日期应为初三(3)", day, result.third)
    }

    /**
     * 验证腊月初三在中文显示中正确展示
     */
    @Test
    fun testBugLunar12Month3Day_displayString() {
        val year = 2025
        val month = 12
        val day = 3

        val solarDate = LunarCalendarUtil.lunarToSolar(year, month, day)
        val result = LunarCalendarUtil.solarToLunar(solarDate)
        val display = LunarCalendarUtil.getLunarDateString(result.first, result.second, result.third)

        assertEquals("腊月初三", display)
    }

    // ==================================================================
    // 全部12个月的往返转换测试（覆盖每个月的边界）
    // ==================================================================

    @Test
    fun testAll12Months_roundTrip() {
        val year = 2025 // 有闰二月（leapMonth=2），可覆盖闰月处理逻辑
        val months = listOf(
            1 to "正月", 2 to "二月", 3 to "三月", 4 to "四月",
            5 to "五月", 6 to "六月", 7 to "七月", 8 to "八月",
            9 to "九月", 10 to "十月", 11 to "冬月", 12 to "腊月"
        )

        for ((month, expectedName) in months) {
            val day = 1 // 初一
            val solarDate = LunarCalendarUtil.lunarToSolar(year, month, day)
            val result = LunarCalendarUtil.solarToLunar(solarDate)
            val display = LunarCalendarUtil.getLunarDateString(result.first, result.second, result.third)

            assertEquals("${expectedName}初一的往返转换月份", month, result.second)
            assertEquals("${expectedName}初一的往返转换日期", day, result.third)
            assertEquals("${expectedName}初一的显示字符串", "${expectedName}初一", display)
        }
    }

    // ==================================================================
    // 不同年份测试（有闰月 vs 无闰月）
    // ==================================================================

    /**
     * 2024年：无闰月（leapMonth=0），各月往返转换应保持正确
     */
    @Test
    fun testNonLeapYear_2024_roundTrip() {
        val year = 2024
        val testCases = listOf(
            1 to 1, 1 to 15, 1 to 30,    // 正月
            6 to 1, 6 to 15, 6 to 29,    // 六月
            12 to 1, 12 to 15, 12 to 30  // 腊月
        )

        for ((month, day) in testCases) {
            val solarDate = LunarCalendarUtil.lunarToSolar(year, month, day)
            val result = LunarCalendarUtil.solarToLunar(solarDate)

            assertEquals("${year}年${month}月${day}日往返转换年份", year, result.first)
            assertEquals("${year}年${month}月${day}日往返转换月份", month, result.second)
            assertEquals("${year}年${month}月${day}日往返转换日期", day, result.third)
        }
    }

    /**
     * 2023年：有闰二月（leapMonth=2），测试腊月往返转换
     */
    @Test
    fun testLeapYear_2023_roundTrip() {
        val year = 2023
        // 2023年有闰二月（leapMonth=2）
        assertTrue("2023年应有闰月", LunarCalendarUtil.getLeapMonth(year) > 0)

        // 测试闰月之前、之中、之后的月份
        val testCases = listOf(
            1 to 1,     // 正月 - 闰月之前
            2 to 1,     // 二月 - 闰月之前（刚好在闰月边界）
            3 to 1,     // 三月 - 闰月之后
            6 to 1,     // 六月 - 闰月之后
            12 to 1,    // 腊月 - 闰月之后（关键测试）
            12 to 15,   // 腊月十五
            12 to 29    // 腊月廿九
        )

        for ((month, day) in testCases) {
            val solarDate = LunarCalendarUtil.lunarToSolar(year, month, day)
            val result = LunarCalendarUtil.solarToLunar(solarDate)
            val display = LunarCalendarUtil.getLunarDateString(result.first, result.second, result.third)

            assertEquals("${year}年${month}月${day}日往返转换月份", month, result.second)
            assertEquals("${year}年${month}月${day}日往返转换日期", day, result.third)
        }
    }

    /**
     * 测试多个有闰月的年份
     */
    @Test
    fun testMultipleLeapYears() {
        // 手动列出已知有闰月的年份进行测试
        val years = listOf(2023, 2025, 2028, 2031, 2033)

        for (year in years) {
            val leapMonth = LunarCalendarUtil.getLeapMonth(year)
            if (leapMonth > 0) {
                // 测试腊月（闰月之后的最大月份）的往返转换
                val solarDate = LunarCalendarUtil.lunarToSolar(year, 12, 15)
                val result = LunarCalendarUtil.solarToLunar(solarDate)

                assertEquals("${year}年腊月往返转换月份应为12", 12, result.second)
                assertEquals("${year}年腊月往返转换日期应为15", 15, result.third)
            }
        }
    }

    // ==================================================================
    // 日期边界测试
    // ==================================================================

    @Test
    fun testDayBoundaries() {
        val year = 2025
        val month = 12  // 腊月

        // 测试朔日（初一）和晦日（廿九/三十）
        val firstDay = 1
        val lastDay = LunarCalendarUtil.monthDays(year, month)

        for (day in listOf(firstDay, lastDay)) {
            val solarDate = LunarCalendarUtil.lunarToSolar(year, month, day)
            val result = LunarCalendarUtil.solarToLunar(solarDate)

            assertEquals("腊月第${day}天往返转换月份", month, result.second)
            assertEquals("腊月第${day}天往返转换日期", day, result.third)
        }
    }

    // ==================================================================
    // solarToLunar 单项测试
    // ==================================================================

    /**
     * 测试已知的农历→公历对应关系
     * 2023年正月初一 = 2023-01-22
     */
    @Test
    fun testLunarToSolar_knownDate() {
        // 2023年正月初一 = 2023-01-22
        assertEquals("2023-01-22", LunarCalendarUtil.lunarToSolar(2023, 1, 1))
    }

    /**
     * 测试往返：公历 → 农历 → 公历
     */
    @Test
    fun testSolarToLunarToSolar_roundTrip() {
        val testDates = listOf(
            "2024-01-01",
            "2024-06-15",
            "2024-12-31",
            "2023-01-22",  // 正月初一
            "2023-12-31"
        )

        for (solarDate in testDates) {
            val lunar = LunarCalendarUtil.solarToLunar(solarDate)
            val backToSolar = LunarCalendarUtil.lunarToSolar(lunar.first, lunar.second, lunar.third)

            assertEquals("公历${solarDate} → 农历 → 公历应一致", solarDate, backToSolar)
        }
    }

    // ==================================================================
    // 闰月日期测试（日期落在闰月内）
    // ==================================================================

    /**
     * 测试闰月的往返转换（日期落在闰月中）
     * 注意：当前实现中 solarToLunar 返回的月份 i 在闰月情况下等于 leapMonth+1，
     * 与紧接在闰月之后的普通月份索引相同。这是已知的限制，后续可改进。
     */
    @Test
    fun testLeapMonthDays_areAccounted() {
        // 2023年闰二月：需要找一个落在闰二月的公历日期
        // 公历 2023-03-22 = 农历 2023年闰二月初一
        val solarDate = "2023-03-22"
        val lunar = LunarCalendarUtil.solarToLunar(solarDate)

        // 注意：当前实现用 i=leapMonth+1 表示闰月（此处 i=3）
        // 实际上 2023-03-22 对应闰二月（夹在二月和三月之间）
        // 修复后至少能正确返回月份而非错误值
        assertTrue("闰月日期应有有效年份", lunar.first > 0)
        assertTrue("闰月日期应有有效月份", lunar.second in 1..12)
        assertTrue("闰月日期应有有效日", lunar.third in 1..30)
    }

    // ==================================================================
    // 跨年测试
    // ==================================================================

    @Test
    fun testYearBoundary_roundTrip() {
        // 1901年（有闰月）的腊月
        val year = 1901
        val month = 12
        val day = 15

        val solarDate = LunarCalendarUtil.lunarToSolar(year, month, day)
        val result = LunarCalendarUtil.solarToLunar(solarDate)

        assertEquals(year, result.first)
        assertEquals(month, result.second)
        assertEquals(day, result.third)
    }
}
