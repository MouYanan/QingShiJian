package com.example.shijian2.util

import org.junit.Assert.*
import org.junit.Test

class PinyinUtilTest {

    // ===== 基础拼音排序测试 =====

    @Test
    fun testChineseTitleSort() {
        val titles = listOf("张三", "李四", "王五", "赵六")
        val sorted = titles.sortedBy { PinyinUtil.getSortKey(it) }
        // 李(L) < 王(W) < 张(Z) < 赵(Z)
        assertEquals("李四", sorted[0])
        assertEquals("王五", sorted[1])
        // 张和赵都是Z开头，按Collator细分
        assertTrue(sorted[2] == "张三" || sorted[2] == "赵六")
        assertTrue(sorted[3] == "赵六" || sorted[3] == "张三")
    }

    @Test
    fun testChineseTitleSortDescending() {
        val titles = listOf("张三", "李四", "王五", "赵六")
        val sorted = titles.sortedByDescending { PinyinUtil.getSortKey(it) }
        // 倒序：赵/张 > 王 > 李
        assertTrue(sorted[0] == "张三" || sorted[0] == "赵六")
        assertEquals("王五", sorted[2])
        assertEquals("李四", sorted[3])
    }

    // ===== 英文标题排序测试 =====

    @Test
    fun testEnglishTitleSort() {
        val titles = listOf("Zoo", "Apple", "Middle", "Banana")
        val sorted = titles.sortedBy { PinyinUtil.getSortKey(it) }
        assertEquals("Apple", sorted[0])
        assertEquals("Banana", sorted[1])
        assertEquals("Middle", sorted[2])
        assertEquals("Zoo", sorted[3])
    }

    @Test
    fun testEnglishCaseInsensitive() {
        val titles = listOf("apple", "Banana", "Cherry")
        val sorted = titles.sortedBy { PinyinUtil.getSortKey(it) }
        assertEquals("apple", sorted[0])
        assertEquals("Banana", sorted[1])
        assertEquals("Cherry", sorted[2])
    }

    // ===== 数字开头标题排序测试 =====

    @Test
    fun testNumericTitleSort() {
        val titles = listOf("3-Third", "1-First", "2-Second", "10-Tenth")
        val sorted = titles.sortedBy { PinyinUtil.getSortKey(it) }
        // 数字排在最前
        assertEquals("1-First", sorted[0])
        assertEquals("10-Tenth", sorted[1])
        assertEquals("2-Second", sorted[2])
        assertEquals("3-Third", sorted[3])
    }

    // ===== 特殊字符标题排序测试 =====

    @Test
    fun testSpecialCharTitleSort() {
        val titles = listOf("中文", "@特殊", "English", "1数字")
        val sorted = titles.sortedBy { PinyinUtil.getSortKey(it) }
        // 顺序：数字 > 英文 > 中文 > 特殊字符
        assertEquals("1数字", sorted[0])
        assertEquals("English", sorted[1])
        assertEquals("中文", sorted[2])
        assertEquals("@特殊", sorted[3])
    }

    // ===== 混合类型标题排序测试 =====

    @Test
    fun testMixedTitleSort() {
        val titles = listOf("笔记", "A-Note", "1-First", "#Tag", "工作")
        val sorted = titles.sortedBy { PinyinUtil.getSortKey(it) }
        // 数字 < 英文 < 中文 < 特殊字符
        assertEquals("1-First", sorted[0])
        assertEquals("A-Note", sorted[1])
        // 中文按拼音排
        assertTrue(sorted[2] == "笔记" || sorted[2] == "工作")
        assertTrue(sorted[3] == "工作" || sorted[3] == "笔记")
        assertEquals("#Tag", sorted[4])
    }

    // ===== 空字符串和边界测试 =====

    @Test
    fun testEmptyString() {
        val key = PinyinUtil.getSortKey("")
        // 空字符串排在最后
        assertEquals("\uFFFF", key)
    }

    @Test
    fun testSingleChar() {
        val titles = listOf("中", "A", "1", "@")
        val sorted = titles.sortedBy { PinyinUtil.getSortKey(it) }
        assertEquals("1", sorted[0])
        assertEquals("A", sorted[1])
        assertEquals("中", sorted[2])
        assertEquals("@", sorted[3])
    }

    // ===== 性能测试（50+条数据） =====

    @Test
    fun testPerformanceWith50Items() {
        val titles = mutableListOf<String>()
        // 生成50条中文标题
        val chineseChars = listOf("阿", "波", "次", "得", "额", "佛", "哥", "喝", "机", "科",
            "勒", "摸", "呢", "哦", "破", "七", "日", "斯", "特", "乌",
            "西", "衣", "资", "安", "本", "查", "德", "恩", "方", "光")
        for (i in 0 until 50) {
            titles.add("${chineseChars[i % chineseChars.size]}项目${i + 1}")
        }

        val startTime = System.currentTimeMillis()
        val sorted = titles.sortedBy { PinyinUtil.getSortKey(it) }
        val elapsed = System.currentTimeMillis() - startTime

        assertEquals(50, sorted.size)
        // 排序应在100ms内完成（远低于300ms要求）
        assertTrue("排序耗时 ${elapsed}ms，超过100ms", elapsed < 100)
    }

    @Test
    fun testPerformanceWith100Items() {
        val titles = mutableListOf<String>()
        for (i in 0 until 100) {
            titles.add("测试项目${i + 1}")
        }

        val startTime = System.currentTimeMillis()
        val sorted = titles.sortedBy { PinyinUtil.getSortKey(it) }
        val elapsed = System.currentTimeMillis() - startTime

        assertEquals(100, sorted.size)
        assertTrue("排序耗时 ${elapsed}ms，超过200ms", elapsed < 200)
    }

    // ===== 排序稳定性测试 =====

    @Test
    fun testSortStability() {
        val titles = listOf("苹果", "香蕉", "苹果", "橙子")
        val sorted = titles.sortedBy { PinyinUtil.getSortKey(it) }
        // 两个"苹果"应保持原始顺序（稳定排序）
        assertEquals("苹果", sorted[0])
        assertEquals("苹果", sorted[1])
    }

    // ===== Collator比较测试 =====

    @Test
    fun testCollatorCompare() {
        // 李(L) < 王(W)
        assertTrue(PinyinUtil.compare("李", "王") < 0)
        // 王(W) < 张(Z)
        assertTrue(PinyinUtil.compare("王", "张") < 0)
    }

    // ===== SortMode枚举测试 =====

    @Test
    fun testSortModeModuleFilter() {
        val noteModes = com.example.shijian2.ui.common.SortMode.entries
            .filter { it.module == "note" }
        assertEquals(2, noteModes.size)
        assertTrue(noteModes.contains(com.example.shijian2.ui.common.SortMode.NOTE_PINYIN_ASC))
        assertTrue(noteModes.contains(com.example.shijian2.ui.common.SortMode.NOTE_PINYIN_DESC))

        val birthdayModes = com.example.shijian2.ui.common.SortMode.entries
            .filter { it.module == "birthday" }
        assertEquals(3, birthdayModes.size)
    }

    // ===== 实际笔记场景测试 =====

    @Test
    fun testRealNoteTitles() {
        val titles = listOf(
            "会议记录",
            "Shopping List",
            "2024年度总结",
            "读书笔记",
            "#重要",
            "旅行计划",
            "API文档",
            "个人日记"
        )

        val sortedAsc = titles.sortedBy { PinyinUtil.getSortKey(it) }
        // 数字开头排最前
        assertEquals("2024年度总结", sortedAsc[0])
        // 英文开头排第二
        assertTrue(sortedAsc[1] == "API文档" || sortedAsc[1] == "Shopping List")
        // 特殊字符排最后
        assertEquals("#重要", sortedAsc.last())

        val sortedDesc = titles.sortedByDescending { PinyinUtil.getSortKey(it) }
        // 倒序：特殊字符最前，数字最后
        assertEquals("#重要", sortedDesc[0])
        assertEquals("2024年度总结", sortedDesc.last())
    }

    // ===== 待办事项排序测试 =====

    @Test
    fun testTodoSortByTitlePinyinAsc() {
        val titles = listOf("完成报告", "安排会议", "发送邮件", "备份文件")
        val sorted = titles.sortedBy { PinyinUtil.getSortKey(it) }
        // 安(A) < 备(B) < 发(F) < 完(W)
        assertEquals("安排会议", sorted[0])
        assertEquals("备份文件", sorted[1])
        assertEquals("发送邮件", sorted[2])
        assertEquals("完成报告", sorted[3])
    }

    @Test
    fun testTodoSortByTitlePinyinDesc() {
        val titles = listOf("完成报告", "安排会议", "发送邮件", "备份文件")
        val sorted = titles.sortedByDescending { PinyinUtil.getSortKey(it) }
        // 完(W) > 发(F) > 备(B) > 安(A)
        assertEquals("完成报告", sorted[0])
        assertEquals("发送邮件", sorted[1])
        assertEquals("备份文件", sorted[2])
        assertEquals("安排会议", sorted[3])
    }

    @Test
    fun testTodoSortWithEmptyTitle() {
        val titles = listOf("任务A", "", "任务B")
        val sorted = titles.sortedBy { PinyinUtil.getSortKey(it) }
        // 空标题排在最后
        assertEquals("任务A", sorted[0])
        assertEquals("任务B", sorted[1])
        assertEquals("", sorted[2])
    }

    @Test
    fun testTodoSortWithSameFirstChar() {
        val titles = listOf("完成任务", "完善文档", "完成报告")
        val sorted = titles.sortedBy { PinyinUtil.getSortKey(it) }
        // 同首字按Collator细分排序
        assertEquals(3, sorted.size)
        // 所有项都以"完"开头
        assertTrue(sorted.all { it.startsWith("完") })
    }

    @Test
    fun testTodoSortWithSpecialChars() {
        val titles = listOf("紧急任务", "!高优先级", "1号任务", "常规工作")
        val sorted = titles.sortedBy { PinyinUtil.getSortKey(it) }
        // 数字 < 中文 < 特殊字符
        assertEquals("1号任务", sorted[0])
        assertEquals("常规工作", sorted[1])
        assertEquals("紧急任务", sorted[2])
        assertEquals("!高优先级", sorted[3])
    }

    @Test
    fun testTodoSortPerformance1000() {
        val titles = mutableListOf<String>()
        val prefixes = listOf("完成", "安排", "发送", "备份", "检查", "更新", "删除", "创建", "修改", "查看")
        for (i in 0 until 1000) {
            titles.add("${prefixes[i % prefixes.size]}任务${i + 1}")
        }

        val startTime = System.currentTimeMillis()
        val sorted = titles.sortedBy { PinyinUtil.getSortKey(it) }
        val elapsed = System.currentTimeMillis() - startTime

        assertEquals(1000, sorted.size)
        assertTrue("1000条排序耗时 ${elapsed}ms，超过300ms", elapsed < 300)
    }

    @Test
    fun testTodoSortModeModuleFilter() {
        val todoModes = com.example.shijian2.ui.common.SortMode.entries
            .filter { it.module == "todo" }
        assertEquals(2, todoModes.size)
        assertTrue(todoModes.contains(com.example.shijian2.ui.common.SortMode.TODO_PINYIN_ASC))
        assertTrue(todoModes.contains(com.example.shijian2.ui.common.SortMode.TODO_PINYIN_DESC))
    }

    @Test
    fun testTodoRealScenario() {
        val titles = listOf(
            "写周报",
            "Review PR",
            "3点开会",
            "#紧急",
            "整理文档",
            "Buy groceries",
            "修复Bug",
            "部署上线"
        )

        val sortedAsc = titles.sortedBy { PinyinUtil.getSortKey(it) }
        // 数字最前
        assertEquals("3点开会", sortedAsc[0])
        // 英文次之
        assertTrue(sortedAsc[1] == "Buy groceries" || sortedAsc[1] == "Review PR")
        // 特殊字符最后
        assertEquals("#紧急", sortedAsc.last())

        val sortedDesc = titles.sortedByDescending { PinyinUtil.getSortKey(it) }
        assertEquals("#紧急", sortedDesc[0])
        assertEquals("3点开会", sortedDesc.last())
    }
}
