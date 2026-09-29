package com.cs2.tracker.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * ByMykel/CSGO-API 物品目录客户端（zh-CN）：
 * 拉取 crates / skins / graffiti，精简为 CatalogItem 缓存到本地，供添加物品与搜索使用。
 * 多镜像源依次尝试（jsDelivr / raw.githubusercontent / fastly），提高可达性。
 */
object CsgoApi {
    private const val CATALOG_FILE = "catalog_v3.json"

    private val mirrors = listOf(
        "https://cdn.jsdelivr.net/gh/ByMykel/CSGO-API@main/public/api/zh-CN/",
        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/api/zh-CN/",
        "https://fastly.jsdelivr.net/gh/ByMykel/CSGO-API@main/public/api/zh-CN/"
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    private lateinit var appContext: Context

    sealed class State {
        data object NotLoaded : State()
        data class Loading(val step: String) : State()
        data class Ready(val items: List<CatalogItem>, val at: Long) : State()
        data class Error(val msg: String) : State()
    }

    private val _state = MutableStateFlow<State>(State.NotLoaded)
    val state: StateFlow<State> = _state

    private var items: List<CatalogItem> = emptyList()

    fun init(context: Context) {
        appContext = context.applicationContext
        val f = appContext.getFileStreamPath(CATALOG_FILE)
        if (f != null && f.exists()) {
            runCatching {
                items = json.decodeFromString<List<CatalogItem>>(f.readText())
                _state.value = State.Ready(items, f.lastModified())
            }
        }
    }

    /** 无缓存时加载；已有缓存直接用 */
    suspend fun ensure() {
        if (_state.value is State.Ready || _state.value is State.Loading) return
        refresh()
    }

    /** 强制重新下载并刷新缓存 */
    suspend fun refresh() = withContext(Dispatchers.IO) {
        _state.value = State.Loading("准备下载…")
        val result = runCatching {
            val crates = fetch("crates.json") { parseGeneric(it, "case") }
            _state.value = State.Loading("皮肤数据…")
            val skins = fetch("skins.json") { parseGeneric(it, "skin") }
            _state.value = State.Loading("涂鸦/挂件…")
            val graffiti = fetch("graffiti.json") { parseGeneric(it, "graffiti") }
            val keychains = fetch("keychains.json") { parseGeneric(it, "keychain") }
            _state.value = State.Loading("饰品数据…")
            val music = fetch("music_kits.json") { parseGeneric(it, "other") }
            val patches = fetch("patches.json") { parseGeneric(it, "other") }
            val agents = fetch("agents.json") { parseGeneric(it, "other") }
            val all = crates + skins + graffiti + keychains + music + patches + agents
            items = all
            appContext.openFileOutput(CATALOG_FILE, Context.MODE_PRIVATE)
                .use { it.write(json.encodeToString(all).toByteArray()) }
            _state.value = State.Ready(all, System.currentTimeMillis())
            all.size
        }
        if (result.isFailure) {
            _state.value = State.Error(result.exceptionOrNull()?.message ?: "网络错误")
        }
    }

    private fun fetch(file: String, parser: (String) -> List<CatalogItem>): List<CatalogItem> {
        var lastErr: Throwable? = null
        for (base in mirrors) {
            try {
                val req = Request.Builder().url(base + file).header("User-Agent", "cs2-tracker").build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) error("HTTP ${resp.code}")
                    return parser(resp.body!!.string())
                }
            } catch (e: Exception) { lastErr = e }
        }
        throw lastErr ?: IllegalStateException("全部镜像不可达: $file")
    }

    private fun rar(o: JsonObject?): Pair<String, String> {
        val r = o ?: return "无" to "#6B7280"
        return (r["name"]?.jsonPrimitive?.contentOrNull ?: "无") to (r["color"]?.jsonPrimitive?.contentOrNull ?: "#6B7280")
    }

    private fun parseGeneric(text: String, cat: String): List<CatalogItem> = buildList {
        json.parseToJsonElement(text).jsonArray.forEach { o ->
            o as JsonObject
            val (rn, rc) = rar(o["rarity"] as? JsonObject)
            // 武器皮肤：读取官方 category.name 作为细分类（手枪/步枪/微型冲锋枪/重型武器/匕首/手套/装备）
            val sub = if (cat == "skin")
                (o["category"] as? JsonObject)?.get("name")?.jsonPrimitive?.contentOrNull ?: ""
            else ""
            add(CatalogItem(
                id = o["id"]!!.jsonPrimitive.content,
                name = o["name"]!!.jsonPrimitive.content,
                cat = cat,
                subCat = sub,
                rarityName = rn, rarityColor = rc,
                img = o["image"]?.jsonPrimitive?.contentOrNull ?: ""
            ))
        }
    }

    fun byCategory(cat: String): List<CatalogItem> = items.filter { it.cat == cat }

    fun search(q: String, limit: Int = 60): List<CatalogItem> {
        if (q.isBlank()) return emptyList()
        val lower = q.trim().lowercase()
        return items.filter { it.name.lowercase().contains(lower) }.take(limit)
    }

    /** 常用可掉落武器箱（快捷打卡） */
    fun quickCases(): List<CatalogItem> {
        val active = listOf(
            "千瓦武器箱", "梦魇武器箱", "反冲武器箱", "变革武器箱", "裂空武器箱",
            "地平线武器箱", "封装的创世终端机", "封装的毁灭之手终端机"
        )
        return active.mapNotNull { n -> items.firstOrNull { it.cat == "case" && it.name == n } }
            .ifEmpty { items.filter { it.cat == "case" && it.name.endsWith("武器箱") }.take(8) }
    }
}
