package com.cs2.tracker.data

import kotlinx.serialization.Serializable

/** 掉落记录（字段与 Web 版备份完全兼容；img/rcolor 为新增可选字段） */
@Serializable
data class Drop(
    val id: String,
    val name: String,
    val date: String,          // yyyy-MM-dd
    val qty: Int = 1,
    val price: Double = 0.0,   // 到手价；负数 = 支出（如开箱花费）
    val note: String = "",
    val category: String = "case",  // case/skin/sticker/graffiti/other
    val rarity: String = "base",
    val img: String? = null,        // Steam CDN 图片
    val rcolor: String? = null      // 品质颜色 #RRGGBB
)

@Serializable
data class AppSettings(
    val periodStart: String = "2025-04-02",
    val periodDays: Int = 7
)

/** 应用数据文件（filesDir/data.json） */
@Serializable
data class AppData(
    val version: Int = 1,
    val settings: AppSettings = AppSettings(),
    val drops: List<Drop> = emptyList(),
    val favorites: Set<String> = emptySet()  // 收藏的物品 id
)

/** 备份文件（与 Web 版导入导出兼容） */
@Serializable
data class Backup(
    val version: Int = 1,
    val exportedAt: String = "",
    val settings: AppSettings? = null,
    val drops: List<Drop> = emptyList()
)

/** 物品目录条目（CSGO-API 精简缓存） */
@Serializable
data class CatalogItem(
    val id: String,
    val name: String,        // 中文名
    val cat: String,         // case / skin / graffiti / keychain / other
    val subCat: String = "", // 武器细分类（手枪/步枪/微型冲锋枪/重型武器/匕首/手套/装备）
    val rarityName: String,
    val rarityColor: String,
    val img: String
)

/** 武器细分类（与 CSGO-API skins.category 一致） */
object WeaponCats {
    val list = listOf("步枪", "手枪", "微型冲锋枪", "重型武器", "匕首", "手套", "装备")
}

/** 品质元信息 */
object RarityMeta {
    val labels = mapOf(
        "consumer" to "消费级", "industrial" to "工业级", "mil-spec" to "军规级",
        "restricted" to "受限", "classified" to "保密", "covert" to "隐秘",
        "rare" to "特殊", "base" to "无"
    )
}

object CategoryMeta {
    val labels = mapOf(
        "case" to "武器箱", "skin" to "皮肤", "sticker" to "贴纸",
        "graffiti" to "涂鸦", "other" to "其他"
    )
    val emojis = mapOf(
        "case" to "📦", "skin" to "🔫", "sticker" to "🏷️", "graffiti" to "🎨", "other" to "🎁"
    )
}

fun money(v: Double): String {
    val s = if (v == v.toLong().toDouble()) v.toLong().toString()
    else String.format(java.util.Locale.CHINA, "%.2f", v)
    return s
}
