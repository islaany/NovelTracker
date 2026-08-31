package com.huqi.noveltracker.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.huqi.noveltracker.data.settings.Presets
import com.huqi.noveltracker.data.settings.SearchBackend

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavHostController,
    viewModel: SettingsViewModel = viewModel()
) {
    val draft by viewModel.draft.collectAsState()
    var showKey by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("API 设置") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("AI 服务商预设", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { viewModel.applyPreset(Presets.deepSeek) }) {
                    Text("DeepSeek（联网搜索）")
                }
                OutlinedButton(onClick = { viewModel.applyPreset(Presets.siliconFlow) }) {
                    Text("SiliconFlow（免费合成）")
                }
                OutlinedButton(onClick = { viewModel.applyPreset(Presets.tavilyWeb) }) {
                    Text("Tavily 联网")
                }
                OutlinedButton(onClick = { viewModel.applyPreset(Presets.exaWeb) }) {
                    Text("Exa 联网")
                }
            }

            OutlinedTextField(
                value = draft.apiKey,
                onValueChange = { newValue -> viewModel.update { it.copy(apiKey = newValue) } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("API Key") },
                singleLine = true,
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showKey = !showKey }) {
                        Icon(
                            if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "显示/隐藏 Key"
                        )
                    }
                }
            )

            OutlinedTextField(
                value = draft.baseUrl,
                onValueChange = { newValue -> viewModel.update { it.copy(baseUrl = newValue) } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Base URL") },
                singleLine = true
            )

            OutlinedTextField(
                value = draft.chatModel,
                onValueChange = { newValue -> viewModel.update { it.copy(chatModel = newValue) } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("对话模型 (chat model)") },
                singleLine = true
            )

            OutlinedTextField(
                value = draft.searchModel,
                onValueChange = { newValue -> viewModel.update { it.copy(searchModel = newValue) } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("搜索模型 (仅 DeepSeek 联网用)") },
                singleLine = true
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "启用联网搜索（web_search，仅 DeepSeek 生效）",
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = draft.webEnabled,
                    onCheckedChange = { checked -> viewModel.update { it.copy(webEnabled = checked) } }
                )
            }

            Text("联网搜索后端（不依赖 DeepSeek 也能真联网）", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val backends = listOf(
                    "无" to SearchBackend.NONE,
                    "Tavily" to SearchBackend.TAVILY,
                    "Exa" to SearchBackend.EXA
                )
                backends.forEach { (label, backend) ->
                    FilterChip(
                        selected = draft.searchBackend == backend,
                        onClick = { viewModel.update { it.copy(searchBackend = backend) } },
                        label = { Text(label) }
                    )
                }
            }

            if (draft.searchBackend != SearchBackend.NONE) {
                OutlinedTextField(
                    value = draft.searchApiKey,
                    onValueChange = { newValue -> viewModel.update { it.copy(searchApiKey = newValue) } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("搜索 API Key（Tavily / Exa）") },
                    singleLine = true,
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation()
                )
                OutlinedTextField(
                    value = draft.searchMaxResults.toString(),
                    onValueChange = { newValue ->
                        newValue.toIntOrNull()?.let { n -> viewModel.update { it.copy(searchMaxResults = n.coerceIn(1, 10)) } }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("检索条数（1-10）") },
                    singleLine = true
                )
                Text(
                    "该后端会真实联网检索，把网页片段交给上面的“对话模型”（如免费 SiliconFlow）合成。" +
                        "仍需在上方 API Key 填入一个可用的对话模型 Key（合成用）。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            Button(
                onClick = { viewModel.save(); navController.popBackStack() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存")
            }

            Text(
                "说明：本应用不再预填任何 Key，首次使用请在「对话模型」处填入一个可用的 LLM Key（如免费 SiliconFlow 做合成）。" +
                    "想要真·联网搜索有两条路：① 点「DeepSeek（联网搜索）」预设并填你自己的 DeepSeek key、打开联网开关；" +
                    "② 点「Tavily 联网」或「Exa 联网」预设，再分别填入搜索 API Key 与一个对话模型 Key，全程免费且真联网。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}
