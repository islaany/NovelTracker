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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.huqi.noveltracker.ui.component.TagChip
import com.huqi.noveltracker.ui.navigation.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNovelScreen(
    navController: NavHostController,
    viewModel: AddNovelViewModel = viewModel()
) {
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
            StepHeader(step = viewModel.step.value)
            Spacer(modifier = Modifier.height(12.dp))

            when (viewModel.step.value) {
                AddStep.PICK -> PickStep { launcher.launch("image/*") }
                AddStep.OCR -> OcrStep()
                AddStep.NAME -> NameStep(
                    viewModel = viewModel,
                    onRerun = {
                        viewModel.imageUri.value?.let { viewModel.onImagePicked(it) }
                    }
                )
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
    val labels = listOf("选择截图", "书名", "生成记录")
    val current = when (step) {
        AddStep.PICK -> 0
        AddStep.OCR, AddStep.NAME -> 1
        AddStep.REVIEW -> 2
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEachIndexed { index, label ->
            val active = index <= current
            Text(
                text = "${index + 1}. $label",
                style = MaterialTheme.typography.labelMedium,
                color = if (active) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
private fun PickStep(onPick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Button(onClick = onPick) { Text("从相册选择截图") }
    }
}

@Composable
private fun OcrStep() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(12.dp))
            Text("正在识别文字…", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun NameStep(viewModel: AddNovelViewModel, onRerun: () -> Unit) {
    val ocrText = viewModel.ocrText.value
    val title = viewModel.title.value
    val uri = viewModel.imageUri.value

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (uri != null) {
            AsyncImage(
                model = uri,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        }
        Text("OCR 识别出的原文（可核对）：", style = MaterialTheme.typography.labelMedium)
        Text(
            text = ocrText.ifBlank { "（未识别到文字，可手动填写书名）" },
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .verticalScroll(rememberScrollState())
                .clip(RoundedCornerShape(8.dp))
                .padding(8.dp)
        )
        OutlinedTextField(
            value = title,
            onValueChange = viewModel::onTitleChange,
            label = { Text("提取出的书名") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onRerun, modifier = Modifier.weight(1f)) { Text("重新识别") }
            Button(
                onClick = viewModel::onSearch,
                modifier = Modifier.weight(1f),
                enabled = title.isNotBlank()
            ) {
                Icon(Icons.Default.Search, contentDescription = null)
                Text(" 搜索并生成记录")
            }
        }
    }
}

@Composable
private fun ReviewStep(viewModel: AddNovelViewModel, onSave: () -> Unit) {
    val d = viewModel.draft.value
    val catalog = viewModel.tags.value.map { it.name }.toSet()
    val displayTags = (catalog + viewModel.selectedTags.value).toList()

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (viewModel.isSearching.value) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        LabeledField("书名", d.title) { newText ->
            viewModel.updateDraft { it.copy(title = newText) }
        }
        LabeledField("作者", d.author) { newText ->
            viewModel.updateDraft { it.copy(author = newText) }
        }
        LabeledField("简介", d.synopsis, multiline = true) { newText ->
            viewModel.updateDraft { it.copy(synopsis = newText) }
        }
        LabeledField("主角", d.protagonist) { newText ->
            viewModel.updateDraft { it.copy(protagonist = newText) }
        }
        LabeledField("高光内容", d.highlights, multiline = true) { newText ->
            viewModel.updateDraft { it.copy(highlights = newText) }
        }

        Text("标签", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            displayTags.forEach { name ->
                val selected = name in viewModel.selectedTags.value
                TagChip(name = name, selected = selected, onClick = { viewModel.toggleTag(name) })
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("想再看一遍", modifier = Modifier.weight(1f))
            Switch(checked = viewModel.wantReRead.value, onCheckedChange = { viewModel.toggleWantReRead() })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("想推荐给别人", modifier = Modifier.weight(1f))
            Switch(checked = viewModel.wantRecommend.value, onCheckedChange = { viewModel.toggleWantRecommend() })
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
