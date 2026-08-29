package com.huqi.noveltracker.ui.screen.add

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.huqi.noveltracker.ui.component.TagChip
import com.huqi.noveltracker.ui.navigation.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNovelScreen(
    navController: NavHostController,
    viewModel: AddNovelViewModel = viewModel()
) {
    val step by viewModel.step.collectAsState()
    val importPhase by viewModel.importPhase.collectAsState()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.onImagePicked(uri)
    }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("添加小说") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            StepHeader(step = step)
            Spacer(modifier = Modifier.height(12.dp))

            when (step) {
                AddStep.PICK -> PickStep { launcher.launch("image/*") }
                AddStep.IMPORTING -> ImportingStep(phase = importPhase)
                AddStep.REVIEW -> ReviewStep(
                    viewModel = viewModel,
                    onSave = {
                        scope.launch {
                            val id = viewModel.save()
                            if (id != null) {
                                navController.navigate(Screen.Detail.createRoute(id)) {
                                    popUpTo(Screen.Home.route)
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun StepHeader(step: AddStep) {
    val labels = listOf("选择截图", "完善记录")
    val current = if (step == AddStep.REVIEW) 1 else 0
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        labels.forEachIndexed { index, label ->
            val reached = index <= current
            val isCurrent = index == current
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(
                        if (reached) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (index < current) "✓" else "${index + 1}",
                    color = if (reached) Color.White
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (isCurrent) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            if (index < labels.lastIndex) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .background(
                            if (index < current) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                )
            }
        }
    }
}

@Composable
private fun PickStep(onPick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(
                onClick = onPick,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text("导入小说截图")
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "选择一张阅读软件里的截图，自动识别书名并生成记录",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ImportingStep(phase: ImportPhase) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = when (phase) {
                    ImportPhase.OCR -> "正在导入…\n识别截图文字中"
                    ImportPhase.SEARCH -> "正在导入…\n生成书籍资料中"
                    ImportPhase.DONE -> "✅ 导入完成"
                },
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ReviewStep(viewModel: AddNovelViewModel, onSave: () -> Unit) {
    val d by viewModel.draft.collectAsState()
    val catalog = viewModel.tags.collectAsState().value.map { it.name }.toSet()
    val selectedTags by viewModel.selectedTags.collectAsState()
    val wantReRead by viewModel.wantReRead.collectAsState()
    val wantRecommend by viewModel.wantRecommend.collectAsState()
    val displayTags = (catalog + selectedTags).toList()

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // "导入完成" 反馈横幅
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "导入完成，可修改后保存到书架",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        LabeledField("书名", d.title) { new -> viewModel.updateDraft { it.copy(title = new) } }
        LabeledField("作者", d.author) { new -> viewModel.updateDraft { it.copy(author = new) } }
        LabeledField("简介", d.synopsis, multiline = true) { new -> viewModel.updateDraft { it.copy(synopsis = new) } }
        LabeledField("主角", d.protagonist) { new -> viewModel.updateDraft { it.copy(protagonist = new) } }
        LabeledField("高光内容", d.highlights, multiline = true) { new -> viewModel.updateDraft { it.copy(highlights = new) } }

        Text("标签", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            displayTags.forEach { name ->
                val selected = name in selectedTags
                TagChip(name = name, selected = selected, onClick = { viewModel.toggleTag(name) })
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("想再看一遍", modifier = Modifier.weight(1f))
            Switch(checked = wantReRead, onCheckedChange = { viewModel.toggleWantReRead() })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("想推荐给别人", modifier = Modifier.weight(1f))
            Switch(checked = wantRecommend, onCheckedChange = { viewModel.toggleWantRecommend() })
        }

        OutlinedButton(
            onClick = viewModel::reImport,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Text(" 重新导入这张截图")
        }

        Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Check, contentDescription = null)
            Text(" 保存到书架")
        }
    }
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    multiline: Boolean = false,
    onValueChange: (String) -> Unit
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = !multiline,
            minLines = if (multiline) 3 else 1
        )
    }
}
