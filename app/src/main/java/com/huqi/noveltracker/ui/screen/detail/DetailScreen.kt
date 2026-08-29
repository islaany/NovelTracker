package com.huqi.noveltracker.ui.screen.detail

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.huqi.noveltracker.data.model.Novel
import com.huqi.noveltracker.ui.component.CoverPlaceholder
import com.huqi.noveltracker.ui.component.SectionCard
import com.huqi.noveltracker.ui.component.TagChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    navController: NavHostController,
    novelId: Long,
    viewModel: DetailViewModel = viewModel(
        factory = DetailViewModel.Factory(
            LocalContext.current.applicationContext as Application,
            novelId
        )
    )
) {
    val novel by viewModel.novel.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("小说详情") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.delete { navController.popBackStack() }
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "删除")
                    }
                }
            )
        }
    ) { padding ->
        novel?.let { current ->
            DetailContent(
                novel = current,
                viewModel = viewModel,
                modifier = Modifier.padding(padding)
            )
        } ?: Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Text("记录不存在或已删除")
        }
    }
}

@Composable
private fun DetailContent(novel: Novel, viewModel: DetailViewModel, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(width = 96.dp, height = 138.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (novel.coverUrl != null) {
                    AsyncImage(
                        model = novel.coverUrl,
                        contentDescription = novel.title,
                        modifier = Modifier.size(width = 96.dp, height = 138.dp)
                    )
                } else {
                    CoverPlaceholder(title = novel.title, width = 96.dp, height = 138.dp)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(novel.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (!novel.author.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("作者：${novel.author}", style = MaterialTheme.typography.bodyLarge)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    novel.tags.forEach { TagChip(name = it) }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (novel.wantReRead == true)
                Button(onClick = viewModel::toggleReRead, modifier = Modifier.weight(1f)) {
                    Text("✓ 想再看")
                }
            else
                OutlinedButton(onClick = viewModel::toggleReRead, modifier = Modifier.weight(1f)) {
                    Text("想再看")
                }
            if (novel.wantRecommend == true)
                Button(onClick = viewModel::toggleRecommend, modifier = Modifier.weight(1f)) {
                    Text("✓ 想推荐")
                }
            else
                OutlinedButton(onClick = viewModel::toggleRecommend, modifier = Modifier.weight(1f)) {
                    Text("想推荐")
                }
        }

        if (!novel.synopsis.isNullOrBlank())
            SectionCard(title = "简介", body = novel.synopsis)
        if (!novel.protagonist.isNullOrBlank())
            SectionCard(title = "主角 / 主要人物", body = novel.protagonist)
        if (!novel.highlights.isNullOrBlank())
            SectionCard(title = "高光内容", body = novel.highlights)
        if (!novel.source.isNullOrBlank()) {
            Text(
                text = "来源：${novel.source}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}
