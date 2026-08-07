package com.example.shijian2.ui.search

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.shijian2.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// 搜索结果数据类
data class SearchResult(
    val bills: List<Bill> = emptyList(),
    val todos: List<Todo> = emptyList(),
    val notes: List<Note> = emptyList(),
    val birthdays: List<Birthday> = emptyList()
)

// 搜索结果项的统一包装
sealed class SearchItem {
    abstract val typeLabel: String
    abstract val identifier: String

    data class BillItem(val bill: Bill) : SearchItem() {
        override val typeLabel = if (bill is ProjectBill) "项目账单" else "时间账单"
        override val identifier = bill.identifier
    }
    data class TodoItem(val todo: Todo) : SearchItem() {
        override val typeLabel = "待办事项"
        override val identifier = todo.identifier
    }
    data class NoteItem(val note: Note) : SearchItem() {
        override val typeLabel = "笔记"
        override val identifier = note.identifier
    }
    data class BirthdayItem(val birthday: Birthday) : SearchItem() {
        override val typeLabel = "生日"
        override val identifier = birthday.identifier
    }
}

/**
 * 搜索结果内容组件（集成到主界面Scaffold中使用）
 * 不包含独立的搜索栏和Scaffold，由DynamicTopAppBar提供搜索输入
 */
@Composable
fun SearchResultsContent(
    query: String,
    onResultClick: (SearchItem) -> Unit,
    selectedTab: Int = 0,
    onTabChange: (Int) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var searchResult by remember { mutableStateOf(SearchResult()) }
    var isSearching by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // 分页状态
    val pageSize = 10
    var displayedBillCount by remember { mutableStateOf(pageSize) }
    var displayedTodoCount by remember { mutableStateOf(pageSize) }
    var displayedNoteCount by remember { mutableStateOf(pageSize) }
    var displayedBirthdayCount by remember { mutableStateOf(pageSize) }

    // 防抖搜索：300ms延迟
    LaunchedEffect(query) {
        if (query.isBlank()) {
            searchResult = SearchResult()
            isSearching = false
            return@LaunchedEffect
        }
        isSearching = true
        delay(300)
        coroutineScope.launch {
            val result = performSearch(context, query.trim())
            searchResult = result
            // 重置分页
            displayedBillCount = pageSize
            displayedTodoCount = pageSize
            displayedNoteCount = pageSize
            displayedBirthdayCount = pageSize
            isSearching = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 分类筛选标签栏
        val tabs = listOf("账单", "待办", "笔记", "生日")
        val tabIcons = listOf(
            Icons.Default.AccountBalance,
            Icons.Default.CheckCircle,
            Icons.AutoMirrored.Filled.StickyNote2,
            Icons.Default.Cake
        )
        val tabResultCounts = listOf(
            searchResult.bills.size,
            searchResult.todos.size,
            searchResult.notes.size,
            searchResult.birthdays.size
        )

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            edgePadding = 16.dp,
            divider = {
                HorizontalDivider(
                    thickness = 2.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            },
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        height = 3.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { onTabChange(index) },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                tabIcons[index],
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                            if (query.isNotBlank() && tabResultCounts[index] > 0) {
                                Badge {
                                    Text(
                                        tabResultCounts[index].toString(),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 搜索结果区域
        if (isSearching) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (query.isBlank()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "输入关键词开始搜索",
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            val hasResultsForTab = when (selectedTab) {
                0 -> searchResult.bills.isNotEmpty()
                1 -> searchResult.todos.isNotEmpty()
                2 -> searchResult.notes.isNotEmpty()
                3 -> searchResult.birthdays.isNotEmpty()
                else -> false
            }
            val hasAnyResults = searchResult.bills.isNotEmpty() || searchResult.todos.isNotEmpty() ||
                searchResult.notes.isNotEmpty() || searchResult.birthdays.isNotEmpty()

            if (!hasAnyResults) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("未找到相关结果", color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "试试其他关键词",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            } else if (!hasResultsForTab) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "该分类下无匹配结果",
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "试试切换其他分类",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            } else {
                val listState = rememberLazyListState()
                val keyword = query.trim()

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // 根据选中的Tab只显示对应分类的结果
                    when (selectedTab) {
                        0 -> { // 账单
                            item(key = "bill_header") {
                                SectionHeader(title = "账单", count = searchResult.bills.size)
                            }
                            items(
                                items = searchResult.bills.take(displayedBillCount),
                                key = { "bill_${it.identifier}" }
                            ) { bill ->
                                BillSearchItem(
                                    bill = bill,
                                    keyword = keyword,
                                    onClick = { onResultClick(SearchItem.BillItem(bill)) }
                                )
                            }
                            if (searchResult.bills.size > displayedBillCount) {
                                item(key = "bill_more") {
                                    LoadMoreButton(
                                        remaining = searchResult.bills.size - displayedBillCount,
                                        onClick = { displayedBillCount += pageSize }
                                    )
                                }
                            }
                        }
                        1 -> { // 待办
                            item(key = "todo_header") {
                                SectionHeader(title = "待办事项", count = searchResult.todos.size)
                            }
                            items(
                                items = searchResult.todos.take(displayedTodoCount),
                                key = { "todo_${it.identifier}" }
                            ) { todo ->
                                TodoSearchItem(
                                    todo = todo,
                                    keyword = keyword,
                                    onClick = { onResultClick(SearchItem.TodoItem(todo)) }
                                )
                            }
                            if (searchResult.todos.size > displayedTodoCount) {
                                item(key = "todo_more") {
                                    LoadMoreButton(
                                        remaining = searchResult.todos.size - displayedTodoCount,
                                        onClick = { displayedTodoCount += pageSize }
                                    )
                                }
                            }
                        }
                        2 -> { // 笔记
                            item(key = "note_header") {
                                SectionHeader(title = "笔记", count = searchResult.notes.size)
                            }
                            items(
                                items = searchResult.notes.take(displayedNoteCount),
                                key = { "note_${it.identifier}" }
                            ) { note ->
                                NoteSearchItem(
                                    note = note,
                                    keyword = keyword,
                                    onClick = { onResultClick(SearchItem.NoteItem(note)) }
                                )
                            }
                            if (searchResult.notes.size > displayedNoteCount) {
                                item(key = "note_more") {
                                    LoadMoreButton(
                                        remaining = searchResult.notes.size - displayedNoteCount,
                                        onClick = { displayedNoteCount += pageSize }
                                    )
                                }
                            }
                        }
                        3 -> { // 生日
                            item(key = "birthday_header") {
                                SectionHeader(title = "生日", count = searchResult.birthdays.size)
                            }
                            items(
                                items = searchResult.birthdays.take(displayedBirthdayCount),
                                key = { "birthday_${it.identifier}" }
                            ) { birthday ->
                                BirthdaySearchItem(
                                    birthday = birthday,
                                    keyword = keyword,
                                    onClick = { onResultClick(SearchItem.BirthdayItem(birthday)) }
                                )
                            }
                            if (searchResult.birthdays.size > displayedBirthdayCount) {
                                item(key = "birthday_more") {
                                    LoadMoreButton(
                                        remaining = searchResult.birthdays.size - displayedBirthdayCount,
                                        onClick = { displayedBirthdayCount += pageSize }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onResultClick: (SearchItem) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var query by remember { mutableStateOf("") }
    var searchResult by remember { mutableStateOf(SearchResult()) }
    var isSearching by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // 分页状态
    val pageSize = 10
    var displayedBillCount by remember { mutableStateOf(pageSize) }
    var displayedTodoCount by remember { mutableStateOf(pageSize) }
    var displayedNoteCount by remember { mutableStateOf(pageSize) }
    var displayedBirthdayCount by remember { mutableStateOf(pageSize) }

    // 防抖搜索：300ms延迟
    LaunchedEffect(query) {
        if (query.isBlank()) {
            searchResult = SearchResult()
            isSearching = false
            return@LaunchedEffect
        }
        isSearching = true
        delay(300)
        coroutineScope.launch {
            val result = performSearch(context, query.trim())
            searchResult = result
            // 重置分页
            displayedBillCount = pageSize
            displayedTodoCount = pageSize
            displayedNoteCount = pageSize
            displayedBirthdayCount = pageSize
            isSearching = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 搜索栏
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = query,
                    onQueryChange = { query = it },
                    onSearch = { /* 已通过LaunchedEffect实时搜索 */ },
                    expanded = true,
                    onExpandedChange = { if (!it) onBack() },
                    placeholder = { Text("搜索账单、待办、笔记、生日...") },
                    leadingIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "清除")
                            }
                        }
                    },
                    interactionSource = remember { MutableInteractionSource() }
                )
            },
            expanded = true,
            onExpandedChange = { if (!it) onBack() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            // 搜索结果区域
            if (isSearching) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (query.isBlank()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "输入关键词开始搜索",
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else if (searchResult.bills.isEmpty() && searchResult.todos.isEmpty() &&
                searchResult.notes.isEmpty() && searchResult.birthdays.isEmpty()
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("未找到相关结果", color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "试试其他关键词",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            } else {
                val listState = rememberLazyListState()
                val keyword = query.trim()

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // 账单分区
                    if (searchResult.bills.isNotEmpty()) {
                        item(key = "bill_header") {
                            SectionHeader(title = "账单", count = searchResult.bills.size)
                        }
                        items(
                            items = searchResult.bills.take(displayedBillCount),
                            key = { "bill_${it.identifier}" }
                        ) { bill ->
                            BillSearchItem(
                                bill = bill,
                                keyword = keyword,
                                onClick = { onResultClick(SearchItem.BillItem(bill)) }
                            )
                        }
                        if (searchResult.bills.size > displayedBillCount) {
                            item(key = "bill_more") {
                                LoadMoreButton(
                                    remaining = searchResult.bills.size - displayedBillCount,
                                    onClick = { displayedBillCount += pageSize }
                                )
                            }
                        }
                    }

                    // 待办分区
                    if (searchResult.todos.isNotEmpty()) {
                        item(key = "todo_header") {
                            SectionHeader(title = "待办事项", count = searchResult.todos.size)
                        }
                        items(
                            items = searchResult.todos.take(displayedTodoCount),
                            key = { "todo_${it.identifier}" }
                        ) { todo ->
                            TodoSearchItem(
                                todo = todo,
                                keyword = keyword,
                                onClick = { onResultClick(SearchItem.TodoItem(todo)) }
                            )
                        }
                        if (searchResult.todos.size > displayedTodoCount) {
                            item(key = "todo_more") {
                                LoadMoreButton(
                                    remaining = searchResult.todos.size - displayedTodoCount,
                                    onClick = { displayedTodoCount += pageSize }
                                )
                            }
                        }
                    }

                    // 笔记分区
                    if (searchResult.notes.isNotEmpty()) {
                        item(key = "note_header") {
                            SectionHeader(title = "笔记", count = searchResult.notes.size)
                        }
                        items(
                            items = searchResult.notes.take(displayedNoteCount),
                            key = { "note_${it.identifier}" }
                        ) { note ->
                            NoteSearchItem(
                                note = note,
                                keyword = keyword,
                                onClick = { onResultClick(SearchItem.NoteItem(note)) }
                            )
                        }
                        if (searchResult.notes.size > displayedNoteCount) {
                            item(key = "note_more") {
                                LoadMoreButton(
                                    remaining = searchResult.notes.size - displayedNoteCount,
                                    onClick = { displayedNoteCount += pageSize }
                                )
                            }
                        }
                    }

                    // 生日分区
                    if (searchResult.birthdays.isNotEmpty()) {
                        item(key = "birthday_header") {
                            SectionHeader(title = "生日", count = searchResult.birthdays.size)
                        }
                        items(
                            items = searchResult.birthdays.take(displayedBirthdayCount),
                            key = { "birthday_${it.identifier}" }
                        ) { birthday ->
                            BirthdaySearchItem(
                                birthday = birthday,
                                keyword = keyword,
                                onClick = { onResultClick(SearchItem.BirthdayItem(birthday)) }
                            )
                        }
                        if (searchResult.birthdays.size > displayedBirthdayCount) {
                            item(key = "birthday_more") {
                                LoadMoreButton(
                                    remaining = searchResult.birthdays.size - displayedBirthdayCount,
                                    onClick = { displayedBirthdayCount += pageSize }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 执行搜索
private suspend fun performSearch(context: android.content.Context, query: String): SearchResult {
    val billRepo = BillRepository(context)
    val todoRepo = TodoRepository(context)
    val noteRepo = NoteRepository(context)
    val birthdayRepo = BirthdayRepository(context)

    val bills = billRepo.getAllBills().first()
    val todos = todoRepo.getAllTodos().first()
    val notes = noteRepo.getAllNotes().first()
    val birthdays = birthdayRepo.getAllBirthdays().first()

    val lowerQuery = query.lowercase()

    return SearchResult(
        bills = bills.filter { bill ->
            when (bill) {
                is ProjectBill -> bill.name.lowercase().contains(lowerQuery) ||
                    bill.expenses.any { it.item.lowercase().contains(lowerQuery) } ||
                    bill.createdAt.lowercase().contains(lowerQuery) ||
                    bill.expenses.any { it.date?.lowercase()?.contains(lowerQuery) == true }
                is TimeBill -> bill.date.lowercase().contains(lowerQuery) ||
                    bill.expenses.any { it.item.lowercase().contains(lowerQuery) } ||
                    bill.createdAt.lowercase().contains(lowerQuery) ||
                    bill.expenses.any { it.date?.lowercase()?.contains(lowerQuery) == true }
            }
        },
        todos = todos.filter { todo ->
            todo.title.lowercase().contains(lowerQuery) ||
                todo.description?.lowercase()?.contains(lowerQuery) == true ||
                todo.startDate.lowercase().contains(lowerQuery) ||
                todo.dueDate.lowercase().contains(lowerQuery) ||
                todo.priority.lowercase().contains(lowerQuery) ||
                todo.status.lowercase().contains(lowerQuery)
        },
        notes = notes.filter { note ->
            note.title.lowercase().contains(lowerQuery) ||
                note.content.lowercase().contains(lowerQuery) ||
                note.createdAt.lowercase().contains(lowerQuery)
        },
        birthdays = birthdays.filter { birthday ->
            birthday.name.lowercase().contains(lowerQuery) ||
                birthday.date.lowercase().contains(lowerQuery) ||
                birthday.relation?.lowercase()?.contains(lowerQuery) == true ||
                birthday.notes?.lowercase()?.contains(lowerQuery) == true
        }
    )
}

// 高亮关键词
fun highlightText(text: String, keyword: String): AnnotatedString {
    if (keyword.isBlank()) return AnnotatedString(text)
    val lowerText = text.lowercase()
    val lowerKeyword = keyword.lowercase()
    val highlightColor = Color(0xFF1565C0)

    return buildAnnotatedString {
        var lastIndex = 0
        var searchStart = 0
        while (searchStart < lowerText.length) {
            val index = lowerText.indexOf(lowerKeyword, searchStart)
            if (index == -1) break
            append(text.substring(lastIndex, index))
            withStyle(SpanStyle(background = Color(0xFFBBDEFB), color = highlightColor, fontWeight = FontWeight.Bold)) {
                append(text.substring(index, index + keyword.length))
            }
            lastIndex = index + keyword.length
            searchStart = index + keyword.length
        }
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
}

// 截取包含关键词的文本片段
fun getSnippet(text: String, keyword: String, contextChars: Int = 30): String {
    if (keyword.isBlank() || text.length <= contextChars * 2 + keyword.length) return text
    val lowerText = text.lowercase()
    val index = lowerText.indexOf(keyword.lowercase())
    if (index == -1) return text.take(contextChars * 2) + "..."
    val start = maxOf(0, index - contextChars)
    val end = minOf(text.length, index + keyword.length + contextChars)
    val prefix = if (start > 0) "..." else ""
    val suffix = if (end < text.length) "..." else ""
    return prefix + text.substring(start, end) + suffix
}

// 分区标题
@Composable
private fun SectionHeader(title: String, count: Int) {
    Column {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${count}条结果",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(top = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

// 加载更多按钮
@Composable
private fun LoadMoreButton(remaining: Int, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("加载更多（剩余${remaining}条）")
    }
}

// 账单搜索结果项
@Composable
private fun BillSearchItem(bill: Bill, keyword: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            when (bill) {
                is ProjectBill -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = highlightText(bill.name, keyword),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "项目账单",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "总金额: ¥${String.format("%.2f", bill.total)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = highlightText(bill.createdAt, keyword),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                is TimeBill -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = highlightText("时间账单 - ${bill.date}", keyword),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "时间账单",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "总金额: ¥${String.format("%.2f", bill.total)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = highlightText(bill.createdAt, keyword),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

// 待办搜索结果项
@Composable
private fun TodoSearchItem(todo: Todo, keyword: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = highlightText(todo.title, keyword),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                val statusText = when (todo.status) {
                    "completed" -> "已完成"
                    "in_progress" -> "进行中"
                    else -> "待处理"
                }
                val statusColor = when (todo.status) {
                    "completed" -> MaterialTheme.colorScheme.primary
                    "in_progress" -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.outline
                }
                Text(
                    statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row {
                Text(
                    text = highlightText("截止: ${todo.dueDate}", keyword),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = highlightText("优先级: ${todo.priority}", keyword),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            if (!todo.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = highlightText(getSnippet(todo.description, keyword), keyword),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}

// 笔记搜索结果项
@Composable
private fun NoteSearchItem(note: Note, keyword: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = highlightText(note.title, keyword),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = highlightText(getSnippet(note.content, keyword), keyword),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = highlightText(note.createdAt, keyword),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

// 生日搜索结果项
@Composable
private fun BirthdaySearchItem(birthday: Birthday, keyword: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = highlightText(birthday.name, keyword),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (birthday.calendarType == "lunar") "农历" else "公历",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row {
                Text(
                    text = highlightText(birthday.date, keyword),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                if (!birthday.relation.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = highlightText(birthday.relation, keyword),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            if (!birthday.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = highlightText(getSnippet(birthday.notes, keyword), keyword),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}
