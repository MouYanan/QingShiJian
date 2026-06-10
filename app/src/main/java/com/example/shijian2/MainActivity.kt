package com.example.shijian2

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.shijian2.data.*
import com.example.shijian2.ui.bill.TimeBillEntryScreen
import com.example.shijian2.ui.bill.ProjectBillEntryScreen
import com.example.shijian2.ui.bill.BillDetailScreen
import com.example.shijian2.ui.todo.TodoEntryScreen
import com.example.shijian2.ui.todo.TodoDetailScreen
import com.example.shijian2.ui.note.NoteEntryScreen
import com.example.shijian2.ui.note.NoteDetailScreen
import com.example.shijian2.ui.birthday.BirthdayEntryScreen
import com.example.shijian2.ui.birthday.BirthdayDetailScreen
import com.example.shijian2.ui.settings.SettingsScreen
import com.example.shijian2.ui.settings.DataManagementScreen
import com.example.shijian2.ui.search.SearchScreen
import com.example.shijian2.ui.search.SearchItem
import com.example.shijian2.ui.search.SearchResultsContent
import com.example.shijian2.ui.theme.Shijian2Theme
import com.example.shijian2.ui.common.ListScreen
import com.example.shijian2.ui.common.ListScreenWithExpandedFAB
import com.example.shijian2.ui.common.AnimatedListScreen
import com.example.shijian2.ui.common.DynamicTopAppBar
import com.example.shijian2.ui.common.ScreenType
import com.example.shijian2.ui.common.ItemType
import com.example.shijian2.ui.common.SortMode
import com.example.shijian2.util.LunarCalendarUtil
import com.example.shijian2.util.PinyinUtil
import com.example.shijian2.service.NotificationScheduler
import com.example.shijian2.util.BirthdayDisplayUtil
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // 初始化通知服务
        initializeNotificationService()
        
        setContent {
            val settingsRepo = remember { SettingsRepository(this@MainActivity) }
            var themeMode by remember { mutableStateOf("light") }
            LaunchedEffect(Unit) {
                settingsRepo.getThemeFlow().collect { themeMode = it }
            }
            Shijian2Theme(themeMode = themeMode) {
                MainScreen()
            }
        }
    }
    
    private fun initializeNotificationService() {
        // 启动通知检查服务
        NotificationScheduler.scheduleDailyNotificationCheck(this)
        
        // 如果是Android 13+，请求通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationPermission = Manifest.permission.POST_NOTIFICATIONS
            
            if (ContextCompat.checkSelfPermission(this, notificationPermission) 
                != PackageManager.PERMISSION_GRANTED) {
                
                // 在后台请求权限，不阻塞UI
                requestPermissions(arrayOf(notificationPermission), 1001)
            }
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    var showDataManagement by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var birthdaySortMode by remember { mutableStateOf(SortMode.BIRTHDAY_TIME_DESC) }
    var noteSortMode by remember { mutableStateOf(SortMode.NOTE_PINYIN_ASC) }
    var todoSortMode by remember { mutableStateOf(SortMode.TODO_PINYIN_ASC) }

    // 从 DataStore 恢复排序状态
    LaunchedEffect(Unit) {
        val settingsRepo = SettingsRepository(context)
        settingsRepo.getBirthdaySortFlow().collect { modeName ->
            birthdaySortMode = SortMode.entries.find { it.name == modeName } ?: SortMode.BIRTHDAY_TIME_DESC
        }
    }
    LaunchedEffect(Unit) {
        val settingsRepo = SettingsRepository(context)
        settingsRepo.getNoteSortFlow().collect { modeName ->
            noteSortMode = SortMode.entries.find { it.name == modeName } ?: SortMode.NOTE_PINYIN_ASC
        }
    }
    LaunchedEffect(Unit) {
        val settingsRepo = SettingsRepository(context)
        settingsRepo.getTodoSortFlow().collect { modeName ->
            todoSortMode = SortMode.entries.find { it.name == modeName } ?: SortMode.TODO_PINYIN_ASC
        }
    }
    // 动态导航栏状态
    var currentScreenType by remember { mutableStateOf(ScreenType.MAIN) }
    var currentItemType by remember { mutableStateOf<ItemType?>(null) }
    
    // 各模块的状态变量
    var selectedBill by remember { mutableStateOf<Bill?>(null) }
    var selectedTodo by remember { mutableStateOf<Todo?>(null) }
    var selectedNote by remember { mutableStateOf<Note?>(null) }
    var selectedBirthday by remember { mutableStateOf<Birthday?>(null) }
    
    // 编辑状态变量
    var billToEdit by remember { mutableStateOf<Bill?>(null) }
    var todoToEdit by remember { mutableStateOf<Todo?>(null) }
    var noteToEdit by remember { mutableStateOf<Note?>(null) }
    var birthdayToEdit by remember { mutableStateOf<Birthday?>(null) }
    
    // 显示创建页面状态
    var showProjectBillEntry by remember { mutableStateOf(false) }
    var showTimeBillEntry by remember { mutableStateOf(false) }
    var showTodoEntry by remember { mutableStateOf(false) }
    var showNoteEntry by remember { mutableStateOf(false) }
    var showBirthdayEntry by remember { mutableStateOf(false) }
    
    // 删除确认对话框状态
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 搜索状态
    var searchQuery by remember { mutableStateOf("") }

    if (showDataManagement) {
        DataManagementScreen(
            onBack = { showDataManagement = false }
        )
    } else if (showSettings) {
        SettingsScreen(
            onBack = { showSettings = false },
            onNavigateToDataManagement = { showDataManagement = true }
        )
    } else {
        Scaffold(
            topBar = {
                DynamicTopAppBar(
                    currentScreenType = if (showSearch) ScreenType.SEARCH else currentScreenType,
                    currentItemType = currentItemType,
                    onBack = {
                        if (showSearch) {
                            showSearch = false
                            searchQuery = ""
                        } else {
                            // 返回逻辑：重置到主界面
                            currentScreenType = ScreenType.MAIN
                            currentItemType = null
                            // 重置选中的项目
                            selectedBill = null
                            selectedTodo = null
                            selectedNote = null
                            selectedBirthday = null
                            // 重置创建页面标志，避免返回后仍渲染创建页面
                            showProjectBillEntry = false
                            showTimeBillEntry = false
                            showTodoEntry = false
                            showNoteEntry = false
                            showBirthdayEntry = false
                            // 重置编辑状态
                            billToEdit = null
                            todoToEdit = null
                            noteToEdit = null
                            birthdayToEdit = null
                        }
                    },
                    onEdit = {
                        // 编辑功能：根据当前项目类型执行编辑操作
                        when (currentItemType) {
                            ItemType.PROJECT_BILL, ItemType.TIME_BILL -> {
                                selectedBill?.let { bill ->
                                    billToEdit = bill
                                    if (bill is ProjectBill) {
                                        showProjectBillEntry = true
                                    } else if (bill is TimeBill) {
                                        showTimeBillEntry = true
                                    }
                                }
                            }
                            ItemType.TODO -> {
                                selectedTodo?.let { todo ->
                                    todoToEdit = todo
                                    showTodoEntry = true
                                }
                            }
                            ItemType.NOTE -> {
                                selectedNote?.let { note ->
                                    noteToEdit = note
                                    showNoteEntry = true
                                }
                            }
                            ItemType.BIRTHDAY -> {
                                selectedBirthday?.let { birthday ->
                                    birthdayToEdit = birthday
                                    showBirthdayEntry = true
                                }
                            }
                            else -> {}
                        }
                    },
                    onDelete = {
                        // 删除功能：显示确认对话框
                        showDeleteDialog = true
                    },
                    onSettings = { showSettings = true },
                    onSearch = {
                        showSearch = true
                        searchQuery = ""
                    },
                    showSort = (selectedTab == 3 || selectedTab == 2 || selectedTab == 1) && !showSearch,
                    currentSortMode = when (selectedTab) {
                        3 -> birthdaySortMode
                        2 -> noteSortMode
                        1 -> todoSortMode
                        else -> birthdaySortMode
                    },
                    onSortModeChange = { mode ->
                        when (selectedTab) {
                            3 -> {
                                birthdaySortMode = mode
                                scope.launch { SettingsRepository(context).setBirthdaySort(mode.name) }
                            }
                            2 -> {
                                noteSortMode = mode
                                scope.launch { SettingsRepository(context).setNoteSort(mode.name) }
                            }
                            1 -> {
                                todoSortMode = mode
                                scope.launch { SettingsRepository(context).setTodoSort(mode.name) }
                            }
                        }
                    },
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it }
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.AccountBalance, contentDescription = null) },
                        label = { Text("账本") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                        label = { Text("待办") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.AutoMirrored.Filled.StickyNote2, contentDescription = null) },
                        label = { Text("笔记") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.Cake, contentDescription = null) },
                        label = { Text("生日") }
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (showSearch) {
                    SearchResultsContent(
                        query = searchQuery,
                        onResultClick = { searchItem ->
                            showSearch = false
                            searchQuery = ""
                            when (searchItem) {
                                is SearchItem.BillItem -> {
                                    selectedBill = searchItem.bill
                                    selectedTab = 0
                                }
                                is SearchItem.TodoItem -> {
                                    selectedTodo = searchItem.todo
                                    selectedTab = 1
                                }
                                is SearchItem.NoteItem -> {
                                    selectedNote = searchItem.note
                                    selectedTab = 2
                                }
                                is SearchItem.BirthdayItem -> {
                                    selectedBirthday = searchItem.birthday
                                    selectedTab = 3
                                }
                            }
                        },
                        selectedTab = selectedTab,
                        onTabChange = { selectedTab = it }
                    )
                } else {
                when (selectedTab) {
                    0 -> BillScreen(
                        onScreenTypeChange = { screenType, itemType ->
                            currentScreenType = screenType
                            currentItemType = itemType
                        },
                        selectedBill = selectedBill,
                        onSelectedBillChange = { bill -> selectedBill = bill },
                        billToEdit = billToEdit,
                        onBillToEditChange = { bill -> billToEdit = bill },
                        showProjectBillEntry = showProjectBillEntry,
                        onShowProjectBillEntryChange = { show -> showProjectBillEntry = show },
                        showTimeBillEntry = showTimeBillEntry,
                        onShowTimeBillEntryChange = { show -> showTimeBillEntry = show }
                    )
                    1 -> TodoScreen(
                        onScreenTypeChange = { screenType, itemType ->
                            currentScreenType = screenType
                            currentItemType = itemType
                        },
                        selectedTodo = selectedTodo,
                        onSelectedTodoChange = { todo -> selectedTodo = todo },
                        todoToEdit = todoToEdit,
                        onTodoToEditChange = { todo -> todoToEdit = todo },
                        showTodoEntry = showTodoEntry,
                        onShowTodoEntryChange = { show -> showTodoEntry = show },
                        sortMode = todoSortMode
                    )
                    2 -> NoteScreen(
                        onScreenTypeChange = { screenType, itemType ->
                            currentScreenType = screenType
                            currentItemType = itemType
                        },
                        selectedNote = selectedNote,
                        onSelectedNoteChange = { note -> selectedNote = note },
                        noteToEdit = noteToEdit,
                        onNoteToEditChange = { note -> noteToEdit = note },
                        showNoteEntry = showNoteEntry,
                        onShowNoteEntryChange = { show -> showNoteEntry = show },
                        sortMode = noteSortMode
                    )
                    3 -> BirthdayScreen(
                        onScreenTypeChange = { screenType, itemType ->
                            currentScreenType = screenType
                            currentItemType = itemType
                        },
                        selectedBirthday = selectedBirthday,
                        onSelectedBirthdayChange = { birthday -> selectedBirthday = birthday },
                        birthdayToEdit = birthdayToEdit,
                        onBirthdayToEditChange = { birthday -> birthdayToEdit = birthday },
                        showBirthdayEntry = showBirthdayEntry,
                        onShowBirthdayEntryChange = { show -> showBirthdayEntry = show },
                        sortMode = birthdaySortMode
                    )
                }
                }
            }
        }
    }
    
    // 删除确认对话框
    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            showDeleteDialog = showDeleteDialog,
            onDismiss = { showDeleteDialog = false },
            currentItemType = currentItemType,
            selectedBill = selectedBill,
            selectedTodo = selectedTodo,
            selectedNote = selectedNote,
            selectedBirthday = selectedBirthday,
            onDeleteSuccess = {
                selectedBill = null
                selectedTodo = null
                selectedNote = null
                selectedBirthday = null
                currentScreenType = ScreenType.MAIN
                currentItemType = null
                showDeleteDialog = false
            }
        )
    }
}

// 简化的Screen函数 - 只处理页面状态变化
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillScreen(
    onScreenTypeChange: (ScreenType, ItemType?) -> Unit,
    selectedBill: Bill?,
    onSelectedBillChange: (Bill?) -> Unit,
    billToEdit: Bill?,
    onBillToEditChange: (Bill?) -> Unit,
    showProjectBillEntry: Boolean,
    onShowProjectBillEntryChange: (Boolean) -> Unit,
    showTimeBillEntry: Boolean,
    onShowTimeBillEntryChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bills by remember { mutableStateOf<List<Bill>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        BillRepository(context).getAllBills().collect { billList ->
            bills = billList
        }
    }

    // 当bills列表更新时，同步更新selectedBill以显示最新数据
    val currentSelectedBill = selectedBill?.let { sel ->
        bills.find { it.identifier == sel.identifier } ?: sel
    }

    // 监听页面状态变化
    LaunchedEffect(showProjectBillEntry, showTimeBillEntry, selectedBill) {
        when {
            showProjectBillEntry -> {
                onScreenTypeChange(ScreenType.CREATE, ItemType.PROJECT_BILL)
            }
            showTimeBillEntry -> {
                onScreenTypeChange(ScreenType.CREATE, ItemType.TIME_BILL)
            }
            selectedBill != null -> {
                val itemType = when (selectedBill) {
                    is ProjectBill -> ItemType.PROJECT_BILL
                    is TimeBill -> ItemType.TIME_BILL
                }
                onScreenTypeChange(ScreenType.DETAIL, itemType)
            }
            else -> {
                onScreenTypeChange(ScreenType.MAIN, null)
            }
        }
    }

    if (showProjectBillEntry) {
        ProjectBillEntryScreen(
            onBack = { 
                onShowProjectBillEntryChange(false)
                onBillToEditChange(null)
            },
            onSaveSuccess = { 
                onShowProjectBillEntryChange(false)
                onBillToEditChange(null)
            },
            billToEdit = billToEdit as? ProjectBill
        )
    } else if (showTimeBillEntry) {
        TimeBillEntryScreen(
            onBack = { 
                onShowTimeBillEntryChange(false)
                onBillToEditChange(null)
            },
            onSaveSuccess = { 
                onShowTimeBillEntryChange(false)
                onBillToEditChange(null)
            },
            billToEdit = billToEdit as? TimeBill
        )
    } else if (currentSelectedBill != null) {
        BillDetailScreen(
            bill = currentSelectedBill,
            onBack = { onSelectedBillChange(null) },
            onEdit = { 
                // 传递当前账单数据到编辑页面
                onBillToEditChange(currentSelectedBill)
                if (currentSelectedBill is ProjectBill) {
                    onShowProjectBillEntryChange(true)
                } else if (currentSelectedBill is TimeBill) {
                    onShowTimeBillEntryChange(true)
                }
            }
        )
        } else {
        ListScreenWithExpandedFAB(
            items = bills,
            itemContent = { bill ->
                BillCard(
                    bill = bill,
                    onClick = { onSelectedBillChange(bill) }
                )
            },
            expanded = expanded,
            onExpandedChange = { expanded = it },
            expandedContent = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            expanded = false
                            onShowProjectBillEntryChange(true)
                        }
                    ) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null)
                        Text("项目账单")
                    }
                    ExtendedFloatingActionButton(
                        onClick = {
                            expanded = false
                            onShowTimeBillEntryChange(true)
                        }
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null)
                        Text("时间账单")
                    }
                }
            }
        )
    }
}

@Composable
fun TodoScreen(
    onScreenTypeChange: (ScreenType, ItemType?) -> Unit,
    selectedTodo: Todo?,
    onSelectedTodoChange: (Todo?) -> Unit,
    todoToEdit: Todo?,
    onTodoToEditChange: (Todo?) -> Unit,
    showTodoEntry: Boolean,
    onShowTodoEntryChange: (Boolean) -> Unit,
    sortMode: SortMode = SortMode.TODO_PINYIN_ASC
) {
    val context = LocalContext.current
    var todos by remember { mutableStateOf<List<Todo>>(emptyList()) }

    LaunchedEffect(Unit) {
        TodoRepository(context).getAllTodos().collect { todoList ->
            todos = todoList
        }
    }

    // 应用排序
    val sortedTodos = remember(todos, sortMode) {
        when (sortMode) {
            SortMode.TODO_PINYIN_ASC -> todos.sortedBy { PinyinUtil.getSortKey(it.title) }
            SortMode.TODO_PINYIN_DESC -> todos.sortedByDescending { PinyinUtil.getSortKey(it.title) }
            else -> todos
        }
    }

    // 当todos列表更新时，同步更新selectedTodo以显示最新数据
    val currentSelectedTodo = selectedTodo?.let { sel ->
        todos.find { it.identifier == sel.identifier } ?: sel
    }

    // 监听页面状态变化
    LaunchedEffect(showTodoEntry, selectedTodo) {
        when {
            showTodoEntry -> {
                onScreenTypeChange(ScreenType.CREATE, ItemType.TODO)
            }
            selectedTodo != null -> {
                onScreenTypeChange(ScreenType.DETAIL, ItemType.TODO)
            }
            else -> {
                onScreenTypeChange(ScreenType.MAIN, null)
            }
        }
    }

    if (showTodoEntry) {
        TodoEntryScreen(
            onBack = { 
                onShowTodoEntryChange(false)
                onTodoToEditChange(null)
            },
            onSaveSuccess = { 
                onShowTodoEntryChange(false)
                onTodoToEditChange(null)
            },
            todoToEdit = todoToEdit
        )
    } else if (currentSelectedTodo != null) {
        TodoDetailScreen(
            todo = currentSelectedTodo,
            onBack = { onSelectedTodoChange(null) },
            onEdit = { 
                // 传递当前待办事项数据到编辑页面
                onTodoToEditChange(currentSelectedTodo)
                onShowTodoEntryChange(true)
            }
        )
        } else {
        AnimatedListScreen(
            items = sortedTodos,
            itemContent = { todo ->
                TodoCard(
                    todo = todo,
                    onClick = { onSelectedTodoChange(todo) }
                )
            },
            onAddClick = { onShowTodoEntryChange(true) }
        )
    }
}

@Composable
fun NoteScreen(
    onScreenTypeChange: (ScreenType, ItemType?) -> Unit,
    selectedNote: Note?,
    onSelectedNoteChange: (Note?) -> Unit,
    noteToEdit: Note?,
    onNoteToEditChange: (Note?) -> Unit,
    showNoteEntry: Boolean,
    onShowNoteEntryChange: (Boolean) -> Unit,
    sortMode: SortMode = SortMode.NOTE_PINYIN_ASC
) {
    val context = LocalContext.current
    var notes by remember { mutableStateOf<List<Note>>(emptyList()) }

    LaunchedEffect(Unit) {
        NoteRepository(context).getAllNotes().collect { noteList ->
            notes = noteList
        }
    }

    // 应用排序
    val sortedNotes = remember(notes, sortMode) {
        when (sortMode) {
            SortMode.NOTE_PINYIN_ASC -> notes.sortedBy { PinyinUtil.getSortKey(it.title) }
            SortMode.NOTE_PINYIN_DESC -> notes.sortedByDescending { PinyinUtil.getSortKey(it.title) }
            else -> notes
        }
    }

    // 当notes列表更新时，同步更新selectedNote以显示最新数据
    val currentSelectedNote = selectedNote?.let { sel ->
        notes.find { it.identifier == sel.identifier } ?: sel
    }

    // 监听页面状态变化
    LaunchedEffect(showNoteEntry, selectedNote) {
        when {
            showNoteEntry -> {
                onScreenTypeChange(ScreenType.CREATE, ItemType.NOTE)
            }
            selectedNote != null -> {
                onScreenTypeChange(ScreenType.DETAIL, ItemType.NOTE)
            }
            else -> {
                onScreenTypeChange(ScreenType.MAIN, null)
            }
        }
    }

    if (showNoteEntry) {
        NoteEntryScreen(
            onBack = { 
                onShowNoteEntryChange(false)
                onNoteToEditChange(null)
            },
            onSaveSuccess = { 
                onShowNoteEntryChange(false)
                onNoteToEditChange(null)
            },
            noteToEdit = noteToEdit
        )
    } else if (currentSelectedNote != null) {
        NoteDetailScreen(
            note = currentSelectedNote,
            onBack = { onSelectedNoteChange(null) },
            onEdit = { 
                // 传递当前笔记数据到编辑页面
                onNoteToEditChange(currentSelectedNote)
                onShowNoteEntryChange(true)
            }
        )
        } else {
        AnimatedListScreen(
            items = sortedNotes,
            itemContent = { note ->
                NoteCard(
                    note = note,
                    onClick = { onSelectedNoteChange(note) }
                )
            },
            onAddClick = { onShowNoteEntryChange(true) }
        )
    }
}

@Composable
fun BirthdayScreen(
    onScreenTypeChange: (ScreenType, ItemType?) -> Unit,
    selectedBirthday: Birthday?,
    onSelectedBirthdayChange: (Birthday?) -> Unit,
    birthdayToEdit: Birthday?,
    onBirthdayToEditChange: (Birthday?) -> Unit,
    showBirthdayEntry: Boolean,
    onShowBirthdayEntryChange: (Boolean) -> Unit,
    sortMode: SortMode = SortMode.BIRTHDAY_TIME_DESC
) {
    val context = LocalContext.current
    var birthdays by remember { mutableStateOf<List<Birthday>>(emptyList()) }

    LaunchedEffect(Unit) {
        BirthdayRepository(context).getAllBirthdays().collect { birthdayList ->
            birthdays = birthdayList
        }
    }

    // 应用排序
    val sortedBirthdays = remember(birthdays, sortMode) {
        when (sortMode) {
            SortMode.BIRTHDAY_TIME_ASC -> birthdays.sortedBy { getBirthdaySortKey(it) }
            SortMode.BIRTHDAY_TIME_DESC -> birthdays.sortedByDescending { getBirthdaySortKey(it) }
            SortMode.BIRTHDAY_PINYIN_ASC -> birthdays.sortedBy { PinyinUtil.getSortKey(it.name) }
            else -> birthdays
        }
    }

    // 当birthdays列表更新时，同步更新selectedBirthday以显示最新数据
    val currentSelectedBirthday = selectedBirthday?.let { sel ->
        birthdays.find { it.identifier == sel.identifier } ?: sel
    }

    // 监听页面状态变化
    LaunchedEffect(showBirthdayEntry, selectedBirthday) {
        when {
            showBirthdayEntry -> {
                onScreenTypeChange(ScreenType.CREATE, ItemType.BIRTHDAY)
            }
            selectedBirthday != null -> {
                onScreenTypeChange(ScreenType.DETAIL, ItemType.BIRTHDAY)
            }
            else -> {
                onScreenTypeChange(ScreenType.MAIN, null)
            }
        }
    }

    if (showBirthdayEntry) {
        BirthdayEntryScreen(
            onBack = { 
                onShowBirthdayEntryChange(false)
                onBirthdayToEditChange(null)
            },
            onSaveSuccess = { 
                onShowBirthdayEntryChange(false)
                onBirthdayToEditChange(null)
            },
            birthdayToEdit = birthdayToEdit
        )
    } else if (currentSelectedBirthday != null) {
        BirthdayDetailScreen(
            birthday = currentSelectedBirthday,
            onBack = { onSelectedBirthdayChange(null) },
            onEdit = { 
                // 传递当前生日数据到编辑页面
                onBirthdayToEditChange(currentSelectedBirthday)
                onShowBirthdayEntryChange(true)
            }
        )
        } else {
        ListScreen(
            items = sortedBirthdays,
            itemContent = { birthday ->
                BirthdayCard(
                    birthday = birthday,
                    onClick = { onSelectedBirthdayChange(birthday) }
                )
            },
            onAddClick = { onShowBirthdayEntryChange(true) }
        )
    }
}

// 生日排序键：提取月日用于时间排序，同时支持农历和公历
private fun getBirthdaySortKey(birthday: Birthday): String {
    return try {
        if (birthday.calendarType == "lunar") {
            // 农历：转换为公历后取月日作为排序键
            val lunar = LunarCalendarUtil.solarToLunar(birthday.date)
            // 格式：MM-dd，保证排序正确
            String.format("%02d-%02d", lunar.second, lunar.third)
        } else {
            // 公历：直接取月日
            val parts = birthday.date.split("-")
            if (parts.size >= 3) "${parts[1]}-${parts[2]}" else birthday.date
        }
    } catch (e: Exception) {
        birthday.date
    }
}

// 简化的Card组件
@Composable
fun BillCard(bill: Bill, onClick: () -> Unit) {
    val title = when (bill) {
        is ProjectBill -> bill.name
        is TimeBill -> "时间账单 - ${bill.date}"
    }
    
    val description = when (bill) {
        is ProjectBill -> "总金额: ¥${bill.total}"
        is TimeBill -> "总金额: ¥${bill.total}"
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TodoCard(todo: Todo, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = todo.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = todo.description ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun NoteCard(note: Note, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = note.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun BirthdayCard(birthday: Birthday, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = birthday.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${BirthdayDisplayUtil.getCalendarTypeDisplay(birthday)}: ${BirthdayDisplayUtil.formatBirthdayForDisplay(birthday)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    showDeleteDialog: Boolean,
    onDismiss: () -> Unit,
    currentItemType: ItemType?,
    selectedBill: Bill?,
    selectedTodo: Todo?,
    selectedNote: Note?,
    selectedBirthday: Birthday?,
    onDeleteSuccess: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("确认删除") },
            text = { 
                Text("您确定要删除此项目吗？")
            },
            confirmButton = {
                Button(
                    onClick = {
                        // 执行删除操作
                        scope.launch {
                            when (currentItemType) {
                                ItemType.PROJECT_BILL, ItemType.TIME_BILL -> {
                                    selectedBill?.let { bill ->
                                        BillRepository(context).deleteBill(bill)
                                    }
                                }
                                ItemType.TODO -> {
                                    selectedTodo?.let { todo ->
                                        TodoRepository(context).deleteTodo(todo)
                                    }
                                }
                                ItemType.NOTE -> {
                                    selectedNote?.let { note ->
                                        NoteRepository(context).deleteNote(note)
                                    }
                                }
                                ItemType.BIRTHDAY -> {
                                    selectedBirthday?.let { birthday ->
                                        BirthdayRepository(context).deleteBirthday(birthday)
                                    }
                                }
                                else -> {}
                            }
                            onDeleteSuccess()
                        }
                    }
                ) {
                    Text("是的")
                }
            },
            dismissButton = {
                Button(
                    onClick = onDismiss
                ) {
                    Text("否")
                }
            }
        )
    }
}