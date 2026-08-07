package com.example.shijian2.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.shijian2.data.SettingsRepository
import com.example.shijian2.service.NotificationScheduler
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onNavigateToDataManagement: () -> Unit = {},
    onNavigateToPermissions: () -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { SettingsRepository(context) }
    val coroutineScope = rememberCoroutineScope()

    var theme by remember { mutableStateOf("light") }
    var notifications by remember { mutableStateOf(true) }
    var showThemeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        theme = repository.getTheme()
        notifications = repository.getNotifications()
    }

    var themeInitialized by remember { mutableStateOf(false) }
    var notificationsInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(theme) {
        if (themeInitialized) {
            repository.setTheme(theme)
        } else {
            themeInitialized = true
        }
    }

    LaunchedEffect(notifications) {
        if (notificationsInitialized) {
            repository.setNotifications(notifications)
            // 同步控制 WorkManager 通知任务
            if (notifications) {
                NotificationScheduler.scheduleDailyNotificationCheck(context)
            } else {
                NotificationScheduler.cancelAllNotifications(context)
            }
        } else {
            notificationsInitialized = true
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("选择主题") },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                theme = "light"
                                showThemeDialog = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LightMode,
                            contentDescription = null,
                            tint = if (theme == "light") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            "浅色模式",
                            color = if (theme == "light") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (theme == "light") {
                            RadioButton(selected = true, onClick = null)
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                theme = "dark"
                                showThemeDialog = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.DarkMode,
                            contentDescription = null,
                            tint = if (theme == "dark") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            "深色模式（测试功能）",
                            color = if (theme == "dark") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (theme == "dark") {
                            RadioButton(selected = true, onClick = null)
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                theme = "system"
                                showThemeDialog = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Palette,
                            contentDescription = null,
                            tint = if (theme == "system") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            "跟随系统",
                            color = if (theme == "system") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (theme == "system") {
                            RadioButton(selected = true, onClick = null)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("关闭")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SettingsItem(
                icon = Icons.Default.Palette,
                title = "主题设置",
                subtitle = when (theme) {
                    "light" -> "浅色模式"
                    "dark" -> "深色模式"
                    else -> "跟随系统"
                },
                onClick = { showThemeDialog = true }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SettingsItem(
                icon = Icons.Default.Notifications,
                title = "通知设置",
                subtitle = if (notifications) "已开启" else "已关闭",
                trailing = {
                    Switch(
                        checked = notifications,
                        onCheckedChange = { notifications = it }
                    )
                },
                onClick = {}
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SettingsItem(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                title = "数据管理",
                subtitle = "备份和导入数据",
                onClick = onNavigateToDataManagement
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                "关于",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("轻时笺", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("版本 1.0.3", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "一个功能强大的时间管理和账单管理应用",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "若您在使用过程中遇到任何问题，欢迎随时向作者反馈，感谢您的支持与理解。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "作者邮箱：1401673667@qq.com",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        if (trailing != null) {
            trailing()
        }
    }
}