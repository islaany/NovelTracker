package com.huqi.noveltracker.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.huqi.noveltracker.data.model.Novel
import com.huqi.noveltracker.ui.component.NovelCard
import com.huqi.noveltracker.ui.component.TagChip
import com.huqi.noveltracker.ui.navigation.Screen
import com.huqi.noveltracker.ui.screen.home.SortMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: HomeViewModel = viewModel()
) {
    val novels by viewModel.filteredNovels.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()
    val query by viewModel.query.collectAsState()
    val sortMode by viewModel.sortMode.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("我的书架", color = MaterialTheme.colorScheme.onPrimary)
                        Text(
                            text = "共 ${novels.size} 本",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Tags.route) }) {
                        Icon(
                            Icons.Default.Sell,
                            contentDescription = "标签管理",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Screen.Add.route) }) {
                Icon(Icons.Default.Add, contentDescription = "添加小说")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 搜索框：按书名或标签名匹配
                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { viewModel.setQuery(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("搜索书名或标签，如：玄幻") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setQuery("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "清除")
                                }
                            }
                        }
                    )
                }

                // 排序方式
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SortChip("最近添加", sortMode == SortMode.RECENT) { viewModel.setSortMode(SortMode.RECENT) }
                        SortChip("主标签优先", sortMode == SortMode.MAIN_FIRST) { viewModel.setSortMode(SortMode.MAIN_FIRST) }
                        SortChip("书名", sortMode == SortMode.TITLE) { viewModel.setSortMode(SortMode.TITLE) }
                    }
                }

                // 标签筛选（按题材浏览）
                if (tags.isNotEmpty()) {
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                TagChip(
                                    name = "全部",
                                    selected = selectedTag == null,
                                    onClick = { viewModel.selectTag(null) }
                                )
                            }
                            items(tags) { tag ->
                                TagChip(
                                    name = tag.name,
                                    colorHex = tag.color,
                                    selected = selectedTag == tag.name,
                                    onClick = { viewModel.selectTag(tag.name) }
                                )
                            }
                        }
                    }
                }

                // 列表 / 空态
                if (novels.isEmpty()) {
                    item {
                        val hasFilter = query.isNotBlank() || selectedTag != null
                        EmptyState(
                            modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                            hasFilter = hasFilter,
                            query = query
                        )
                    }
                } else {
                    items(novels, key = { it.id }) { novel: Novel ->
                        NovelCard(
                            novel = novel,
                            onClick = { navController.navigate(Screen.Detail.createRoute(novel.id)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SortChip(label: String, selected: Boolean, onClick: () -> Unit) {
    TagChip(name = label, colorHex = "#7C4DFF", selected = selected, onClick = onClick)
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier, hasFilter: Boolean = false, query: String = "") {
    Text(
        text = if (hasFilter)
            "没有找到匹配${if (query.isNotBlank()) "「$query」" else ""}的书。\n换个关键词或点「全部」看看。"
        else
            "还没有记录。\n点右下角 + 从截图添加第一本吧。",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        modifier = modifier.padding(24.dp)
    )
}
