package com.huqi.noveltracker.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "novel_tracker_settings")

/** Which web-search backend to use for real online lookup (decoupled from the LLM). */
enum class SearchBackend { NONE, TAVILY, EXA }

/**
 * Runtime AI provider configuration. Stored in DataStore (not compiled into the build)
 * so the same APK works with any provider the user picks in the in-app Settings screen.
 *
 * Web search is NOT DeepSeek-exclusive: besides DeepSeek's native `web_search` tool you
 * can plug in a dedicated search API (Tavily / Exa, both have free tiers ~1000/mo) and
 * feed the returned snippets to ANY chat LLM (e.g. free SiliconFlow) for synthesis.
 */
data class SearchConfig(
    val apiKey: String = "",
    val baseUrl: String = "https://api.siliconflow.cn/v1/",
    val chatModel: String = "deepseek-ai/DeepSeek-V4-Flash",
    val searchModel: String = "",
    val webEnabled: Boolean = false,
    val searchBackend: SearchBackend = SearchBackend.NONE,
    val searchApiKey: String = "",
    val searchMaxResults: Int = 5
) {
    /** Web search (Responses API + web_search tool) is a DeepSeek-only capability. */
    val isDeepSeek: Boolean get() = baseUrl.contains("deepseek.com", ignoreCase = true)
}

/**
 * Presets are starting points — the user still pastes their own key(s). The app no longer
 * ships a working free key, so every install requires the user to add at least one key.
 */
object Presets {
    /** Knowledge-only: free SiliconFlow key (user pastes their own) for chat synthesis. */
    val siliconFlow = SearchConfig(
        apiKey = "",
        baseUrl = "https://api.siliconflow.cn/v1/",
        chatModel = "deepseek-ai/DeepSeek-V4-Flash",
        searchModel = "",
        webEnabled = false
    )
    /** DeepSeek native web_search (Responses API). User pastes their DeepSeek key. */
    val deepSeek = SearchConfig(
        apiKey = "",
        baseUrl = "https://api.deepseek.com/v1/",
        chatModel = "deepseek-chat",
        searchModel = "deepseek-v4-flash",
        webEnabled = true
    )
    /**
     * Free + real web search: dedicated search API (Tavily/Exa, free ~1000/mo) returns real
     * web snippets, and the free SiliconFlow LLM synthesizes them into a record. User pastes
     * BOTH a SiliconFlow key (synthesis) and a Tavily/Exa key (search). No DeepSeek needed.
     */
    val tavilyWeb = SearchConfig(
        apiKey = "",
        baseUrl = "https://api.siliconflow.cn/v1/",
        chatModel = "deepseek-ai/DeepSeek-V4-Flash",
        searchModel = "",
        webEnabled = false,
        searchBackend = SearchBackend.TAVILY,
        searchApiKey = "",
        searchMaxResults = 5
    )
    val exaWeb = SearchConfig(
        apiKey = "",
        baseUrl = "https://api.siliconflow.cn/v1/",
        chatModel = "deepseek-ai/DeepSeek-V4-Flash",
        searchModel = "",
        webEnabled = false,
        searchBackend = SearchBackend.EXA,
        searchApiKey = "",
        searchMaxResults = 5
    )
}

private object Keys {
    val API_KEY = stringPreferencesKey("api_key")
    val BASE_URL = stringPreferencesKey("base_url")
    val CHAT_MODEL = stringPreferencesKey("chat_model")
    val SEARCH_MODEL = stringPreferencesKey("search_model")
    val WEB_ENABLED = booleanPreferencesKey("web_enabled")
    val SEARCH_BACKEND = stringPreferencesKey("search_backend")
    val SEARCH_API_KEY = stringPreferencesKey("search_api_key")
    val SEARCH_MAX_RESULTS = intPreferencesKey("search_max_results")
    val CONFIG_SEEDED = booleanPreferencesKey("config_seeded")
}

class SettingsRepository(private val context: Context) {
    val configFlow: Flow<SearchConfig> = context.dataStore.data.map { prefs ->
        SearchConfig(
            apiKey = prefs[Keys.API_KEY] ?: Presets.tavilyWeb.apiKey,
            baseUrl = prefs[Keys.BASE_URL] ?: Presets.tavilyWeb.baseUrl,
            chatModel = prefs[Keys.CHAT_MODEL] ?: Presets.tavilyWeb.chatModel,
            searchModel = prefs[Keys.SEARCH_MODEL] ?: Presets.tavilyWeb.searchModel,
            webEnabled = prefs[Keys.WEB_ENABLED] ?: Presets.tavilyWeb.webEnabled,
            searchBackend = prefs[Keys.SEARCH_BACKEND]?.let { runCatching { SearchBackend.valueOf(it) }.getOrNull() }
                ?: Presets.tavilyWeb.searchBackend,
            searchApiKey = prefs[Keys.SEARCH_API_KEY] ?: Presets.tavilyWeb.searchApiKey,
            searchMaxResults = prefs[Keys.SEARCH_MAX_RESULTS] ?: Presets.tavilyWeb.searchMaxResults
        )
    }

    suspend fun getConfig(): SearchConfig = configFlow.first()

    suspend fun save(config: SearchConfig) {
        context.dataStore.edit { prefs ->
            prefs[Keys.API_KEY] = config.apiKey
            prefs[Keys.BASE_URL] = config.baseUrl
            prefs[Keys.CHAT_MODEL] = config.chatModel
            prefs[Keys.SEARCH_MODEL] = config.searchModel
            prefs[Keys.WEB_ENABLED] = config.webEnabled
            prefs[Keys.SEARCH_BACKEND] = config.searchBackend.name
            prefs[Keys.SEARCH_API_KEY] = config.searchApiKey
            prefs[Keys.SEARCH_MAX_RESULTS] = config.searchMaxResults
        }
    }

    /**
     * On first launch, write the build-time built-in keys (injected via BuildConfig from CI
     * secrets) into DataStore so the app works out-of-the-box without manual setup.
     * Idempotent and SAFE: never overwrites an existing user configuration. If the build
     * provided no keys at all, this is a no-op (the user still configures manually).
     */
    suspend fun seedBuiltInDefaults(siliconFlowKey: String, tavilyKey: String) {
        if (siliconFlowKey.isBlank() && tavilyKey.isBlank()) return
        context.dataStore.edit { prefs ->
            val already = prefs[Keys.CONFIG_SEEDED] ?: false
            val hasApi = (prefs[Keys.API_KEY] ?: "").isNotBlank()
            val hasSearch = (prefs[Keys.SEARCH_API_KEY] ?: "").isNotBlank()
            if (already || hasApi || hasSearch) return@edit
            prefs[Keys.API_KEY] = siliconFlowKey
            prefs[Keys.BASE_URL] = "https://api.siliconflow.cn/v1/"
            prefs[Keys.CHAT_MODEL] = "deepseek-ai/DeepSeek-V4-Flash"
            prefs[Keys.SEARCH_MODEL] = ""
            prefs[Keys.WEB_ENABLED] = false
            prefs[Keys.SEARCH_BACKEND] = SearchBackend.TAVILY.name
            prefs[Keys.SEARCH_API_KEY] = tavilyKey
            prefs[Keys.SEARCH_MAX_RESULTS] = 5
            prefs[Keys.CONFIG_SEEDED] = true
        }
    }

    /** Force-restore the build-time built-in defaults (used by Settings → 恢复默认配置). */
    suspend fun resetToBuiltIn(siliconFlowKey: String, tavilyKey: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.API_KEY] = siliconFlowKey
            prefs[Keys.BASE_URL] = "https://api.siliconflow.cn/v1/"
            prefs[Keys.CHAT_MODEL] = "deepseek-ai/DeepSeek-V4-Flash"
            prefs[Keys.SEARCH_MODEL] = ""
            prefs[Keys.WEB_ENABLED] = false
            prefs[Keys.SEARCH_BACKEND] = SearchBackend.TAVILY.name
            prefs[Keys.SEARCH_API_KEY] = tavilyKey
            prefs[Keys.SEARCH_MAX_RESULTS] = 5
            prefs[Keys.CONFIG_SEEDED] = true
        }
    }
}
