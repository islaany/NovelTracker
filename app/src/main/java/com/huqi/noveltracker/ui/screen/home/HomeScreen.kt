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
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.huqi.noveltracker.data.model.Novel
import com.huqi.noveltracker.ui.component.NovelCard
import com.huqi.noveltracker.ui.component.TagChip
import com.huqi.noveltracker.ui.navigation.Screen
import com.huqi.noveltracker.util.RecommendExporter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: HomeViewModel = viewModel()
) {
    val novels by viewModel.filteredNovels.collectAsState()
    val filterTags by viewModel.filterTags.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()
    val query by viewModel.query.collectAsState()
    val sortMode by viewModel.sortMode.collectAsState()
    val listMode by viewModel.listMode.collectAsState()
    val recommendNovels by viewModel.recommendNovels.collectAsState()

    val drawerState = rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(Modifier.padding(16.dp)) {
                    Text("小说书架", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
                }
                NavigationDrawerItem(
                    label = { Text("我的书架") },
                    icon = { Icon(Icons.Default.MenuBook, contentDescription = null) },
                    selected = listMode == ListMode.ALL,
                    onClick = { viewModel.setListMode(ListMode.ALL); scope.launch { drawerState.close() } }
                )
                NavigationDrawerItem(
                    label = { Text("想再看") },
                    icon = { Icon(Icons.Default.Bookmark, contentDescription = null) },
                    selected = listMode == ListMode.WANT_REREAD,
                    onClick = { viewModel.setListMode(ListMode.WANT_REREAD); scope.launch { drawerState.close() } }
                )
                NavigationDrawerItem(
                    label = { Text("想推荐") },
                    icon = { Icon(Icons.Default.Star, contentDescription = null) },
                    selected = listMode == ListMode.WANT_RECOMMEND,
                    onClick = { viewModel.setListMode(ListMode.WANT_RECOMMEND); scope.launch { drawerState.close() } }
                )
                NavigationDrawerItem(
                    label = { Text("标签管理") },
                    icon = { Icon(Icons.Default.Sell, contentDescription = null) },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() }; navController.navigate(Screen.Tags.route) }
                )
                NavigationDrawerItem(
                    label = { Text("备份与恢复") },
                    icon = { Icon(Icons.Default.Backup, contentDescription = null) },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() }; navController.navigate(Screen.Backup.route) }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = when (listMode) {
                                    ListMode.WANT_REREAD -> "想再看"
                                    ListMode.WANT_RECOMMEND -> "想推荐"
                                    else -> "我的书架"
                                },
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                text = "共 ${novels.size} 本",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "菜单",
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
                            trailingIcon = if (query.isNotEmpty()) {
                                {
                                    IconButton(onClick = { viewModel.setQuery("") }) {
                                        Icon(Icons.Default.Close, contentDescription = "清除")
                                    }
                                }
                            } else null
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

                    // 想推荐模式：一键导出书单
                    if (listMode == ListMode.WANT_RECOMMEND) {
                        item {
                            Button(
                                onClick = {
                                    val uri = RecommendExporter.exportHtml(context, recommendNovels)
                                    if (uri != null) RecommendExporter.share(context, uri)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = recommendNovels.isNotEmpty()
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null)
                                Text(" 导出推荐书单（${recommendNovels.size} 本）")
                            }
                        }
                    }

                    // 标签筛选（只显示用过的标签；点已选标签可取消）
                    if (filterTags.isNotEmpty()) {
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
                                items(filterTags) { tag ->
                                    val isSel = selectedTag == tag.name
                                    TagChip(
                                        name = tag.name,
                                        colorHex = tag.color,
                                        selected = isSel,
                                        onClick = { viewModel.selectTag(if (isSel) null else tag.name) }
                                    )
                                }
                            }
                        }
                    }

                    // 已筛选时，给一个明显的「清除」入口，方便返回找别的标签
                    if (selectedTag != null || query.isNotBlank()) {
                        item {
                            TagChip(
                                name = "✕ 清除筛选",
                                selected = true,
                                onClick = { viewModel.selectTag(null); viewModel.setQuery("") }
                            )
                        }
                    }

                    // 列表 / 空态
                    if (novels.isEmpty()) {
                        item {
                            val hasFilter = query.isNotBlank() || selectedTag != null || listMode != ListMode.ALL
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
}

@Composable
private fun SortChip(label: String, selected: Boolean, onClick: () -> Unit) {
    TagChip(name = label, colorHex = "#7C4DFF", selected = selected, onClick = onClick)
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier, hasFilter: Boolean = false, query: String = "") {
    Text(
        text = if (hasFilter)
            "这里还没有符合条件的书。\n点上方「✕ 清除筛选」回到全部，或换个关键词。"
        else
            "还没有记录。\n点右下角 + 从截图添加第一本吧。",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        modifier = modifier.padding(24.dp)
    )
}
