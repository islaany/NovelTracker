package com.huqi.noveltracker.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: HomeViewModel = viewModel()
) {
    val novels = viewModel.filteredNovels.value
    val tags = viewModel.tags.value
    val selectedTag = viewModel.selectedTag.value

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
            if (novels.isEmpty()) {
                EmptyState(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
private fun EmptyState(modifier: Modifier = Modifier) {
    Text(
        text = "还没有记录。\n点右下角 + 从截图添加第一本吧。",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        modifier = modifier.padding(24.dp)
    )
}
