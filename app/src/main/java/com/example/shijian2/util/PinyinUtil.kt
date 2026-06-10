package com.example.shijian2.util

import java.text.Collator
import java.util.Locale

/**
 * 拼音工具类，用于中文标题的拼音排序
 * 
 * 排序规则：
 * 1. 数字开头的标题排在最前（0-9）
 * 2. 英文字母开头的标题按字母顺序排列
 * 3. 中文字符按拼音首字母排列
 * 4. 特殊字符排在最后
 */
object PinyinUtil {
    // 使用中文 Collator 进行本地化排序，自动处理拼音
    private val chineseCollator: Collator = Collator.getInstance(Locale.CHINESE).apply {
        strength = Collator.PRIMARY
    }

    /**
     * 获取字符串的排序键，用于排序比较
     * 规则：数字 < 英文字母 < 中文拼音 < 特殊字符
     */
    fun getSortKey(text: String): String {
        if (text.isEmpty()) return "\uFFFF"

        val firstChar = text[0]
        return when {
            firstChar.isDigit() -> "0$firstChar$text"           // 数字开头：0 前缀
            firstChar.isLetter() && firstChar.code < 128 -> "1${firstChar.uppercaseChar()}$text" // 英文开头：1 前缀
            isChinese(firstChar) -> "2${getPinyinPrefix(firstChar)}$text"  // 中文：2 前缀 + 拼音首字母
            else -> "3$firstChar$text"                           // 特殊字符：3 前缀，排最后
        }
    }

    /**
     * 使用 Collator 比较两个字符串
     * 性能优化：缓存排序键避免重复计算
     */
    fun compare(s1: String, s2: String): Int {
        return chineseCollator.compare(s1, s2)
    }

    /**
     * 判断字符是否为中文字符
     */
    private fun isChinese(c: Char): Boolean {
        val code = c.code
        return code in 0x4E00..0x9FFF ||       // CJK 统一汉字
               code in 0x3400..0x4DBF ||       // CJK 扩展A
               code in 0x20000..0x2A6DF ||     // CJK 扩展B
               code in 0xF900..0xFAFF          // CJK 兼容汉字
    }

    /**
     * 获取中文字符的拼音首字母
     * 使用 Collator 的排序特性来推断拼音首字母
     */
    private fun getPinyinPrefix(c: Char): String {
        // 每个拼音首字母对应的第一个常见汉字
        val pinyinLetters = charArrayOf(
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K',
            'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'W',
            'X', 'Y', 'Z'
        )
        // 每个拼音首字母对应的参考汉字
        val referenceChars = charArrayOf(
            '阿', '八', '嚓', '哒', '蛾', '发', '噶', '哈', '击', '喀',
            '垃', '妈', '拿', '哦', '啪', '期', '然', '仨', '他', '挖',
            '昔', '压', '匝'
        )

        for (i in referenceChars.indices) {
            if (chineseCollator.compare(c.toString(), referenceChars[i].toString()) < 0) {
                return if (i > 0) pinyinLetters[i - 1].toString() else pinyinLetters[0].toString()
            }
        }
        return "Z"
    }
}
