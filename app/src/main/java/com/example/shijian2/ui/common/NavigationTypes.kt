package com.example.shijian2.ui.common

/**
 * 页面类型枚举
 */
enum class ScreenType {
    MAIN,          // 主界面
    CREATE,        // 创建页面
    DETAIL,        // 详情页面
    SEARCH         // 搜索页面
}

/**
 * 项目类型枚举
 */
enum class ItemType {
    PROJECT_BILL,  // 项目账单
    TIME_BILL,     // 时间账单
    TODO,          // 待办事项
    NOTE,          // 笔记
    BIRTHDAY       // 生日
}

/**
 * 获取项目类型对应的中文标题
 */
fun ItemType.getTitle(): String {
    return when (this) {
        ItemType.PROJECT_BILL -> "项目账单"
        ItemType.TIME_BILL -> "时间账单"
        ItemType.TODO -> "待办事项"
        ItemType.NOTE -> "笔记"
        ItemType.BIRTHDAY -> "生日"
    }
}