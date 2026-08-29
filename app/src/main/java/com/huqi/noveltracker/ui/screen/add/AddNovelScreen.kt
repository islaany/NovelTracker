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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    val error by viewModel.error.collectAsState()
    val balance by viewModel.balance.collectAsState()
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

            // Visible error feedback so a failed import never looks like "nothing happened".
            error?.let {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = viewModel::clearError) {
                            Icon(Icons.Filled.Refresh, contentDescription = "关闭")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            when (step) {
                AddStep.PICK -> PickStep(
                    balance = balance,
                    onPick = { launcher.launch("image/*") },
                    onManual = { viewModel.startManualSearch(it) }
                )
                AddStep.IMPORTING -> ImportingStep(phase = importPhase)
                AddStep.REVIEW -> ReviewStep(
                    viewModel = viewModel,
                    onSave = { action ->
                        scope.launch {
                            val id = viewModel.save(action)
                            if (id != null) {
                                navController.navigate(Screen.Detail.createRoute(id)) {
                                    popUpTo(Screen.Home.route)
                                }
                            }
                        }
                    },
                    onViewExisting = { id ->
                        navController.navigate(Screen.Detail.createRoute(id)) {
                            popUpTo(Screen.Home.route)
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
private fun PickStep(balance: String?, onPick: () -> Unit, onManual: (String) -> Unit) {
    val manualTitle = remember { mutableStateOf("") }
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        balance?.let {
            Text(
                "DeepSeek 余额：¥$it",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
        Button(
            onClick = onPick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("导入小说截图")
        }
        Text(
            text = "选择一张阅读软件里的截图，自动识别书名并生成记录",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        HorizontalDivider()
        Text("或手动输入书名", style = MaterialTheme.typography.labelMedium)
        OutlinedTextField(
            value = manualTitle.value,
            onValueChange = { manualTitle.value = it },
            label = { Text("书名（如：庆余年）") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Button(
            onClick = { onManual(manualTitle.value) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("用 AI 生成记录")
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
                    ImportPhase.SEARCH -> "正在导入…\n联网搜索这本书的资料\n（约 10–30 秒，请稍候）"
                    ImportPhase.DONE -> "✅ 导入完成"
                },
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReviewStep(
    viewModel: AddNovelViewModel,
    onSave: (SaveAction) -> Unit,
    onViewExisting: (Long) -> Unit
) {
    val d by viewModel.draft.collectAsState()
    val ocrText by viewModel.ocrText.collectAsState()
    val catalogTags by viewModel.tags.collectAsState()
    val mainTags by viewModel.mainTags.collectAsState()
    val subTags by viewModel.subTags.collectAsState()
    val wantReRead by viewModel.wantReRead.collectAsState()
    val wantRecommend by viewModel.wantRecommend.collectAsState()
    val tagQuery by viewModel.tagQuery.collectAsState()
    val duplicate by viewModel.duplicate.collectAsState()
    val sources by viewModel.sources.collectAsState()
    val verified by viewModel.verified.collectAsState()
    val colorMap = catalogTags.associate { it.name to it.color }

    val allTagNames = (catalogTags.map { it.name }.toSet() + mainTags + subTags).toList()
    val q = tagQuery.trim()
    // Selected first, unselected after — the point is to see at a glance what the AI
    // picked for you without hunting through ~120 chips.
    val orderedTags = allTagNames
        .filter { q.isEmpty() || it.contains(q, ignoreCase = true) }
        .sortedBy { n -> if (n in mainTags) 0 else if (n in subTags) 1 else 2 }
    val mainFull = mainTags.size >= 2
    val queryHasExactMatch = allTagNames.any { it.equals(q, ignoreCase = true) }

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

        // 重复导入提醒：这本书可能以前导过，避免书架上出现两本一样的
        duplicate?.let { existing ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "书架里已有一本《${existing.title}》",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "添加于 ${formatDate(existing.addedAt)} · 主标签：" +
                            existing.mainTags.joinToString("、").ifBlank { "无" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "保存时默认用新内容更新它，不会多出一本重复的书。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                    TextButton(onClick = { onViewExisting(existing.id) }) {
                        Text("去看已有的那本")
                    }
                }
            }
        }

        // 让 AI 实际看到的「识别原文」可见，便于核对 OCR 是否读对、一起定位问题
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "识别原文（OCR）",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (ocrText.isBlank()) "（未识别到任何文字）" else ocrText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )
            }
        }

        // 没能联网核实 → 明确警示，不让猜测冒充事实
        if (!verified) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "没能联网核实这本书。以下资料可能不准，请自行核对；查不到的字段请手动填写。",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // AI 实际参考的来源，方便自己核对
        if (sources.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "资料来源（AI 联网查证）",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    sources.forEach { s ->
                        Text(
                            "· ${s.substringBefore("|")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        LabeledField("书名", d.title) { new -> viewModel.updateDraft { it.copy(title = new) } }
        LabeledField("作者", d.author) { new -> viewModel.updateDraft { it.copy(author = new) } }
        LabeledField("简介", d.synopsis, multiline = true) { new -> viewModel.updateDraft { it.copy(synopsis = new) } }
        LabeledField("主角", d.protagonist) { new -> viewModel.updateDraft { it.copy(protagonist = new) } }
        LabeledField("高光内容", d.highlights, multiline = true) { new -> viewModel.updateDraft { it.copy(highlights = new) } }

        // ── 标签区：搜索定位 + 已选优先 ─────────────────────────────
        OutlinedTextField(
            value = tagQuery,
            onValueChange = { viewModel.setTagQuery(it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("搜索标签，如：破镜、年下、ABO") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = if (tagQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { viewModel.setTagQuery("") }) {
                        Icon(Icons.Default.Close, contentDescription = "清除")
                    }
                }
            } else null
        )

        Text(
            "主标签（最多 2 个，决定归类与排序优先）",
            style = MaterialTheme.typography.labelMedium
        )
        if (mainTags.isEmpty()) {
            Text(
                "· 还没选主标签：在下方点标签加入副标签后，再点它上面的「↑」升为主标签",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
        if (mainFull) {
            Text(
                "· 主标签已满（最多 2 个），先取消一个可再选",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            mainTags.forEach { name ->
                TagChip(
                    name = name,
                    colorHex = colorMap[name],
                    filled = true,
                    actionLabel = "✕",
                    onAction = { viewModel.toggleMain(name) }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text("副标签（可多个，细化分类）", style = MaterialTheme.typography.labelMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            orderedTags.filter { it !in mainTags }.forEach { name ->
                val isSub = name in subTags
                TagChip(
                    name = name,
                    colorHex = colorMap[name],
                    selected = isSub,
                    onClick = { viewModel.toggleSub(name) },
                    actionLabel = if (isSub) "↑" else null,
                    onAction = if (isSub) ({ viewModel.promoteSubToMain(name) }) else null
                )
            }
        }

        // 库里没有这个词 → 直接新建，并自己决定进主标签还是副标签
        if (q.isNotEmpty() && !queryHasExactMatch) {
            Spacer(modifier = Modifier.height(6.dp))
            Text("库里没有「$q」", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.addCustomTag(q, asMain = true); viewModel.setTagQuery("") },
                    modifier = Modifier.weight(1f)
                ) { Text("＋ 新建并加到主标签") }
                OutlinedButton(
                    onClick = { viewModel.addCustomTag(q, asMain = false); viewModel.setTagQuery("") },
                    modifier = Modifier.weight(1f)
                ) { Text("＋ 新建并加到副标签") }
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

        if (duplicate == null) {
            Button(onClick = { onSave(SaveAction.CREATE) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Check, contentDescription = null)
                Text(" 保存到书架")
            }
        } else {
            Button(onClick = { onSave(SaveAction.UPDATE_EXISTING) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Check, contentDescription = null)
                Text(" 用新内容更新已有的那本（推荐）")
            }
            OutlinedButton(onClick = { onSave(SaveAction.CREATE) }, modifier = Modifier.fillMaxWidth()) {
                Text("仍然存成新的一本")
            }
        }
    }
}

private fun formatDate(ms: Long): String =
    java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        .format(java.util.Date(ms))

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
