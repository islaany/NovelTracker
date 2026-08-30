package com.huqi.noveltracker.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "novel_tracker_settings")

/**
 * Runtime AI provider configuration. Stored in DataStore (not compiled into the build)
 * so the same APK works with any provider the user picks in the in-app Settings screen.
 */
data class SearchConfig(
    val apiKey: String = "",
    val baseUrl: String = "https://api.siliconflow.cn/v1/",
    val chatModel: String = "deepseek-ai/DeepSeek-V4-Flash",
    val searchModel: String = "",
    val webEnabled: Boolean = false
) {
    /** Web search (Responses API + web_search tool) is a DeepSeek-only capability. */
    val isDeepSeek: Boolean get() = baseUrl.contains("deepseek.com", ignoreCase = true)
}

/**
 * Free SiliconFlow key shipped as the default so the app works out-of-the-box.
 * It is revocable/free; switch to your own DeepSeek key in Settings to enable web search.
 * (Shipped in the APK like any default string — acceptable for a free key. If you later
 * make the repo public, prefer clearing this and requiring the user to paste their own.)
 */
const val FREE_SILICONFLOW_KEY = "sk-jkudxsfbjbdukxlbsbprnyjutmruytbclykummuswhzhizll"

object Presets {
    val siliconFlow = SearchConfig(
        apiKey = FREE_SILICONFLOW_KEY,
        baseUrl = "https://api.siliconflow.cn/v1/",
        chatModel = "deepseek-ai/DeepSeek-V4-Flash",
        searchModel = "",
        webEnabled = false
    )
    val deepSeek = SearchConfig(
        apiKey = "",
        baseUrl = "https://api.deepseek.com/v1/",
        chatModel = "deepseek-chat",
        searchModel = "deepseek-v4-flash",
        webEnabled = true
    )
}

private object Keys {
    val API_KEY = stringPreferencesKey("api_key")
    val BASE_URL = stringPreferencesKey("base_url")
    val CHAT_MODEL = stringPreferencesKey("chat_model")
    val SEARCH_MODEL = stringPreferencesKey("search_model")
    val WEB_ENABLED = booleanPreferencesKey("web_enabled")
}

class SettingsRepository(private val context: Context) {
    val configFlow: Flow<SearchConfig> = context.dataStore.data.map { prefs ->
        SearchConfig(
            apiKey = prefs[Keys.API_KEY] ?: Presets.siliconFlow.apiKey,
            baseUrl = prefs[Keys.BASE_URL] ?: Presets.siliconFlow.baseUrl,
            chatModel = prefs[Keys.CHAT_MODEL] ?: Presets.siliconFlow.chatModel,
            searchModel = prefs[Keys.SEARCH_MODEL] ?: Presets.siliconFlow.searchModel,
            webEnabled = prefs[Keys.WEB_ENABLED] ?: Presets.siliconFlow.webEnabled
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
        }
    }
}
