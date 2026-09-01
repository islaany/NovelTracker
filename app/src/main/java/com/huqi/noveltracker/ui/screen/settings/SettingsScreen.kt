package com.huqi.noveltracker.ui.screen.settings

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.huqi.noveltracker.data.settings.SearchBackend
import com.huqi.noveltracker.ui.navigation.Screen

// All settings sub-screens share ONE ViewModel scoped to the Activity, so changes made in a
// sub-screen are visible everywhere and saved together.
@Composable
private fun settingsVm(): SettingsViewModel {
    val activity = LocalContext.current as ComponentActivity
    return viewModel(viewModelStoreOwner = activity)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavHostController) {
    val vm = settingsVm()
    val draft by vm.draft.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
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
            Text("配置中心（按使用频率排序）", style = MaterialTheme.typography.titleMedium)
            Text(
                "想不依赖 DeepSeek 做真·联网检索 —— 用「联网搜索（Tavily / Exa）」即可，不消耗 DeepSeek 额度。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            SettingsRow(
                icon = Icons.Default.Cloud,
                title = "联网搜索（Tavily / Exa）",
                desc = "真·联网查小说资料，无需 DeepSeek、不消耗其额度",
                badge = backendBadge(draft.searchBackend, draft.searchApiKey),
                onClick = { navController.navigate(Screen.SettingsSearch.route) }
            )
            SettingsRow(
                icon = Icons.Default.AutoAwesome,
                title = "AI 模型（资料增强 · 可选）",
                desc = "填对话模型 Key 让资料更完整；不填也能用 Tavily 直接出简介",
                badge = if (draft.apiKey.isNotBlank()) "已配置" else "未配置",
                onClick = { navController.navigate(Screen.SettingsModel.route) }
            )
            SettingsRow(
                icon = Icons.Default.Search,
                title = "DeepSeek 联网（可选）",
                desc = "用 DeepSeek 原生联网；会消耗 DeepSeek 额度，与上面二选一",
                badge = if (draft.webEnabled && draft.isDeepSeek) "已开启" else "未开启",
                onClick = { navController.navigate(Screen.SettingsDeepSeek.route) }
            )
            SettingsRow(
                icon = Icons.Default.Info,
                title = "状态与关于",
                desc = "版本、重置设置",
                badge = "",
                onClick = { navController.navigate(Screen.SettingsAbout.route) }
            )

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { vm.save(); navController.popBackStack() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("保存并返回") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSearchScreen(navController: NavHostController) {
    val vm = settingsVm()
    val draft by vm.draft.collectAsState()
    var showKey by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("联网搜索") },
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
            Text("选择搜索引擎", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BackendChip("无", SearchBackend.NONE, draft.searchBackend) {
                    vm.update { it.copy(searchBackend = SearchBackend.NONE, webEnabled = false) }
                }
                BackendChip("Tavily", SearchBackend.TAVILY, draft.searchBackend) {
                    vm.update { it.copy(searchBackend = SearchBackend.TAVILY, webEnabled = false) }
                }
                BackendChip("Exa", SearchBackend.EXA, draft.searchBackend) {
                    vm.update { it.copy(searchBackend = SearchBackend.EXA, webEnabled = false) }
                }
            }

            if (draft.searchBackend != SearchBackend.NONE) {
                OutlinedTextField(
                    value = draft.searchApiKey,
                    onValueChange = { newValue ->
                        vm.update {
                            it.copy(
                                searchApiKey = newValue,
                                // First key typed auto-selects Tavily so search actually runs.
                                searchBackend = if (it.searchBackend == SearchBackend.NONE && newValue.isNotBlank())
                                    SearchBackend.TAVILY else it.searchBackend
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("搜索 API Key（Tavily / Exa）") },
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
                    value = draft.searchMaxResults.toString(),
                    onValueChange = { newValue ->
                        newValue.toIntOrNull()?.let { n -> vm.update { it.copy(searchMaxResults = n.coerceIn(1, 10)) } }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("检索条数（1-10）") },
                    singleLine = true
                )
                Text(
                    "填入 Tavily / Exa 的 API Key 后，导入截图即可真联网检索小说资料，全程不调用 DeepSeek，也不消耗其额度。" +
                        "可选在「AI 模型」填一个对话模型 Key，让标签 / 简介更完整。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            } else {
                Text(
                    "先选择一个搜索引擎（推荐 Tavily）。选择后需填写对应的 API Key。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            Button(
                onClick = { vm.save(); navController.popBackStack() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("保存") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsModelScreen(navController: NavHostController) {
    val vm = settingsVm()
    val draft by vm.draft.collectAsState()
    var showKey by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI 模型") },
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
            Text("对话模型（资料增强 · 可选）", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = draft.apiKey,
                onValueChange = { newValue -> vm.update { it.copy(apiKey = newValue) } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("API Key（对话模型）") },
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
                onValueChange = { newValue -> vm.update { it.copy(baseUrl = newValue) } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Base URL") },
                singleLine = true
            )
            OutlinedTextField(
                value = draft.chatModel,
                onValueChange = { newValue -> vm.update { it.copy(chatModel = newValue) } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("对话模型 (chat model)") },
                singleLine = true
            )
            Text(
                "留空则不调用任何大模型：Tavily 会直接用搜索结果里的简介，无需 DeepSeek。" +
                    "填了对话模型 Key 后，检索结果会被进一步整理（标签等更完整）。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Button(
                onClick = { vm.save(); navController.popBackStack() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("保存") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDeepSeekScreen(navController: NavHostController) {
    val vm = settingsVm()
    val draft by vm.draft.collectAsState()
    var showKey by remember { mutableStateOf(false) }
    val enabled = draft.webEnabled && draft.isDeepSeek
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DeepSeek 联网") },
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
            Text("DeepSeek 原生联网（可选）", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("启用 DeepSeek 联网搜索（web_search）", modifier = Modifier.weight(1f))
                Switch(
                    checked = enabled,
                    onCheckedChange = { on ->
                        vm.update {
                            it.copy(
                                webEnabled = on,
                                searchBackend = if (on) SearchBackend.NONE else it.searchBackend,
                                baseUrl = if (on) "https://api.deepseek.com/v1/" else it.baseUrl,
                                searchModel = if (on) "deepseek-v4-flash" else it.searchModel
                            )
                        }
                    }
                )
            }
            if (enabled) {
                OutlinedTextField(
                    value = draft.apiKey,
                    onValueChange = { newValue -> vm.update { it.copy(apiKey = newValue) } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("DeepSeek API Key") },
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
                    value = draft.searchModel,
                    onValueChange = { newValue -> vm.update { it.copy(searchModel = newValue) } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("搜索模型 (search model)") },
                    singleLine = true
                )
                Text(
                    "⚠️ 此模式会消耗 DeepSeek 账户额度（你此前因它产生了欠费）。与「联网搜索（Tavily）」二选一，不要同时开启。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                )
            } else {
                Text(
                    "未启用。启用后会使用 DeepSeek 的 web_search 工具联网检索，需要 DeepSeek API Key，并消耗其额度。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            Button(
                onClick = { vm.save(); navController.popBackStack() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("保存") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAboutScreen(navController: NavHostController) {
    val vm = settingsVm()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("状态与关于") },
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
            Text("NovelTracker", style = MaterialTheme.typography.titleMedium)
            Text("版本：0.13.0 (build 49)", style = MaterialTheme.typography.bodyMedium)
            Text(
                "本地离线 OCR：ML Kit 中文识别",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                "联网检索：Tavily / Exa（无需 DeepSeek）或 DeepSeek 原生 web_search（可选）",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            HorizontalDivider()
            Button(
                onClick = {
                    vm.resetToBuiltIn()
                    vm.save()
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("恢复默认配置（含预置 Key）") }
            Text(
                "一键恢复应用内置的「硅基流动 + Tavily」默认配置。若你之后改过 Key 又想退回，点这里即可。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    vm.update { com.huqi.noveltracker.data.settings.SearchConfig() }
                    vm.save()
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors()
            ) { Text("清空全部设置") }
            Text(
                "清空后所有 Key 与后端选择归零，恢复空白（需手动重新配置）。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String,
    badge: String,
    onClick: () -> Unit
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            if (badge.isNotBlank()) {
                AssistChip(onClick = onClick, label = { Text(badge) })
            }
        }
    }
}

@Composable
private fun BackendChip(label: String, backend: SearchBackend, selected: SearchBackend, onClick: () -> Unit) {
    FilterChip(selected = selected == backend, onClick = onClick, label = { Text(label) })
}

private fun backendBadge(b: SearchBackend, key: String): String = when (b) {
    SearchBackend.NONE -> "未选择"
    SearchBackend.TAVILY -> if (key.isNotBlank()) "Tavily 已配置" else "Tavily 未填 Key"
    SearchBackend.EXA -> if (key.isNotBlank()) "Exa 已配置" else "Exa 未填 Key"
}
