package com.example.shijian2.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun <T> ListScreen(
    items: List<T>,
    itemContent: @Composable (T) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items) { item ->
                itemContent(item)
            }
        }

        FloatingActionButton(
            onClick = onAddClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "添加")
        }
    }
}

/**
 * 带动画的列表组件，排序切换时提供平滑过渡
 */
@Composable
fun <T> AnimatedListScreen(
    items: List<T>,
    itemContent: @Composable (T) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                count = items.size,
                key = { index ->
                    val item = items[index]
                    // 使用 identifier 作为 key 保证排序动画正确
                    when (item) {
                        is com.example.shijian2.data.Note -> item.identifier
                        is com.example.shijian2.data.Birthday -> item.identifier
                        is com.example.shijian2.data.Bill -> item.identifier
                        is com.example.shijian2.data.Todo -> item.identifier
                        else -> index
                    }
                }
            ) { index ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(150))
                ) {
                    itemContent(items[index])
                }
            }
        }

        FloatingActionButton(
            onClick = onAddClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "添加")
        }
    }
}

@Composable
fun <T> ListScreenWithExpandedFAB(
    items: List<T>,
    itemContent: @Composable (T) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    expandedContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items) { item ->
                itemContent(item)
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            if (expanded) {
                expandedContent()
            }

            FloatingActionButton(
                onClick = { onExpandedChange(!expanded) }
            ) {
                Icon(
                    if (expanded) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = "添加"
                )
            }
        }
    }
}