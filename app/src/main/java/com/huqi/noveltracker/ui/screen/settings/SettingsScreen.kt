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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.huqi.noveltracker.data.settings.Presets

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
                    Text("SiliconFlow（免费）")
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

            Button(
                onClick = { viewModel.save(); navController.popBackStack() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存")
            }

            Text(
                "说明：默认已填入免费 SiliconFlow key，可直接用（仅知识补全，未联网核实）。" +
                    "想开启真实联网搜索，点「DeepSeek」预设，Base URL 保持默认，并在 API Key 处填入你自己的 DeepSeek key，" +
                    "再打开「启用联网搜索」后保存。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}
