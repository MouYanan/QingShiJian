package com.example.shijian2.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.shijian2.R

enum class SortMode(val label: String, val module: String) {
    // 生日模块排序
    BIRTHDAY_TIME_ASC("按时间排序（正序）", "birthday"),
    BIRTHDAY_TIME_DESC("按时间排序（倒序）", "birthday"),
    BIRTHDAY_PINYIN_ASC("按拼音排序（A-Z）", "birthday"),
    // 笔记模块排序
    NOTE_PINYIN_ASC("按拼音排序（A-Z）", "note"),
    NOTE_PINYIN_DESC("按拼音排序（Z-A）", "note"),
    // 待办模块排序
    TODO_PINYIN_ASC("按拼音排序（A-Z）", "todo"),
    TODO_PINYIN_DESC("按拼音排序（Z-A）", "todo")
}

/**
 * 动态导航栏组件
 * 根据页面类型和项目类型动态显示标题和操作按钮
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicTopAppBar(
    currentScreenType: ScreenType,
    currentItemType: ItemType? = null,
    onBack: () -> Unit = {},
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onSettings: () -> Unit = {},
    onSearch: () -> Unit = {},
    showSort: Boolean = false,
    currentSortMode: SortMode = SortMode.BIRTHDAY_TIME_DESC,
    onSortModeChange: (SortMode) -> Unit = {},
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {}
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }

    if (currentScreenType == ScreenType.SEARCH) {
        // 搜索模式：显示搜索输入框
        TopAppBar(
            title = {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("搜索账单、待办、笔记、生日...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "清除")
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回"
                    )
                }
            }
        )
    } else {
        // 根据页面类型和项目类型生成标题
        val title = when (currentScreenType) {
            ScreenType.MAIN -> stringResource(R.string.app_name)
            ScreenType.CREATE -> currentItemType?.getTitle() ?: "创建"
            ScreenType.DETAIL -> currentItemType?.getTitle() ?: "详情"
            else -> ""
        }

        TopAppBar(
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            navigationIcon = {
                // 只在非主界面显示返回按钮
                if (currentScreenType != ScreenType.MAIN) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            },
            actions = {
                // 根据页面类型显示不同的操作按钮
                when (currentScreenType) {
                    ScreenType.MAIN -> {
                        // 排序按钮（仅在生日/笔记模块显示）
                        if (showSort) {
                            Box {
                                IconButton(onClick = { sortMenuExpanded = true }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Sort,
                                        contentDescription = "排序"
                                    )
                                }
                                DropdownMenu(
                                    expanded = sortMenuExpanded,
                                    onDismissRequest = { sortMenuExpanded = false }
                                ) {
                                    // 只显示当前模块的排序选项
                                    val currentModule = currentSortMode.module
                                    SortMode.entries.filter { it.module == currentModule }
                                        .forEach { mode ->
                                            DropdownMenuItem(
                                                text = {
                                                    Row {
                                                        Text(mode.label)
                                                        if (mode == currentSortMode) {
                                                            Text(
                                                                " ✓",
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                    }
                                                },
                                                onClick = {
                                                    onSortModeChange(mode)
                                                    sortMenuExpanded = false
                                                }
                                            )
                                        }
                                }
                            }
                        }
                        // 搜索按钮
                        IconButton(onClick = onSearch) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "搜索"
                            )
                        }
                        // 设置按钮
                        IconButton(onClick = onSettings) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "设置"
                            )
                        }
                    }
                    ScreenType.CREATE -> {
                        // 创建页面不显示操作按钮
                    }
                    ScreenType.DETAIL -> {
                        // 详情页面显示修改和删除按钮
                        IconButton(onClick = onEdit) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "修改"
                            )
                        }
                        IconButton(onClick = onDelete) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "删除"
                            )
                        }
                    }
                    else -> {}
                }
            }
        )
    }
}