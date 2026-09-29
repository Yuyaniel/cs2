package com.cs2.tracker.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import java.time.LocalDate
import java.util.UUID

/**
 * 掉落数据存储：内存 StateFlow + filesDir/data.json 持久化。
 * 备份导入/导出格式与 Web 版（cs2-drops-backup.json）完全兼容。
 */
object DropStore {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    private lateinit var appContext: Context
    private val _data = MutableStateFlow(AppData())
    val data: StateFlow<AppData> = _data

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast

    fun init(context: Context) {
        appContext = context.applicationContext
        val f = appContext.getFileStreamPath("data.json")
        if (f != null && f.exists()) {
            runCatching {
                _data.value = json.decodeFromString<AppData>(f.readText())
            }.onFailure { _lastError.value = "读取数据失败: ${it.message}" }
        }
    }

    val settings: AppSettings get() = _data.value.settings
    val drops: List<Drop> get() = _data.value.drops

    private fun persist() {
        appContext.openFileOutput("data.json", Context.MODE_PRIVATE).use {
            it.write(json.encodeToString(_data.value).toByteArray())
        }
    }

    fun add(d: Drop) = update(_data.value.copy(drops = _data.value.drops + d))
    fun update(d: Drop) = update(_data.value.copy(drops = _data.value.drops.map { if (it.id == d.id) d else it }))
    fun delete(id: String) = update(_data.value.copy(drops = _data.value.drops.filter { it.id != id }))

    fun saveSettings(s: AppSettings) = update(_data.value.copy(settings = s))

    private fun update(new: AppData) {
        _data.value = new
        persist()
    }

    fun newId(): String = UUID.randomUUID().toString().replace("-", "").take(12)

    fun clearAll() = update(AppData(settings = _data.value.settings))

    // ---- 收藏 ----
    fun isFavorite(id: String): Boolean = id in _data.value.favorites
    fun toggleFavorite(id: String) {
        val cur = _data.value.favorites
        val next = if (id in cur) cur - id else cur + id
        update(_data.value.copy(favorites = next))
    }

    /** 导出 JSON 备份（与 Web 版格式一致） */
    fun exportJson(): String = json.encodeToString(
        Backup(version = 1, exportedAt = LocalDate.now().toString(), settings = _data.value.settings, drops = _data.value.drops)
    )

    /** 导出 CSV（UTF-8 BOM，Excel 友好） */
    fun exportCsv(): String {
        val head = "日期,期,名称,类型,品质,数量,金额,备注"
        val rows = _data.value.drops.sortedWith(compareByDescending<Drop> { it.date }).map { d ->
            listOf(
                d.date, "第 ${Period.of(d.date, _data.value.settings).index} 期", d.name,
                CategoryMeta.labels[d.category] ?: d.category, RarityMeta.labels[d.rarity] ?: d.rarity,
                d.qty.toString(), d.price.toString(), d.note
            ).joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" }
        }
        return "\uFEFF" + (listOf(head) + rows).joinToString("\r\n")
    }

    /** 导入备份（按 id 去重合并），返回提示文本 */
    fun importJson(text: String): String {
        return runCatching {
            val raw = json.parseToJsonElement(text)
            val obj = raw as? kotlinx.serialization.json.JsonObject ?: error("格式错误")
            val dropsArr = obj["drops"] as? kotlinx.serialization.json.JsonArray ?: error("缺少 drops")
            val imported = dropsArr.map { json.decodeFromJsonElement(Drop.serializer(), it) }
            val existing = _data.value.drops.associateBy { it.id }.toMutableMap()
            var added = 0
            imported.forEach { if (!existing.containsKey(it.id)) { existing[it.id] = it; added++ } }
            val newSettings = obj["settings"]?.let { runCatching { json.decodeFromJsonElement(AppSettings.serializer(), it) }.getOrNull() }
            update(AppData(
                version = 1,
                settings = newSettings ?: _data.value.settings,
                drops = existing.values.sortedWith(compareByDescending<Drop> { it.date })
            ))
            "导入完成：共 ${imported.size} 条，新增 $added 条，重复 ${imported.size - added} 条已跳过"
        }.getOrElse { "导入失败：${it.message}" }
    }

    fun consumeToast() { _toast.value = null }
    fun toast(msg: String) { _toast.value = msg }
}
