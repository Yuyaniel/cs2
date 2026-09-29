package com.cs2.tracker.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** 期（周期）计算 —— 与 Web 版逻辑一致 */
object Period {
    private val fmt = DateTimeFormatter.ISO_LOCAL_DATE

    fun parse(s: String): LocalDate = LocalDate.parse(s, fmt)
    fun str(d: LocalDate): String = d.format(fmt)

    fun periodDays(settings: AppSettings): Int = settings.periodDays.coerceAtLeast(1)

    /** 某日期所属的期。index 从 1 开始；早于第 1 期起始日可为 0 或负 */
    fun of(date: LocalDate, settings: AppSettings): Info {
        val start = parse(settings.periodStart)
        val days = periodDays(settings)
        val diff = java.time.temporal.ChronoUnit.DAYS.between(start, date).toInt()
        val idx = Math.floorDiv(diff, days)
        val pStart = start.plusDays(idx.toLong() * days)
        val pEnd = pStart.plusDays((days - 1).toLong())
        val nextStart = pStart.plusDays(days.toLong())
        return Info(
            index = idx + 1,
            key = str(pStart),
            start = pStart,
            end = pEnd,
            nextStart = nextStart,
            daysLeft = java.time.temporal.ChronoUnit.DAYS.between(date, nextStart).toInt(),
            label = "${str(pStart)} ~ ${str(pEnd)}"
        )
    }

    fun of(dateStr: String, settings: AppSettings) = of(parse(dateStr), settings)

    /** 数据中出现过的所有期 key（含当前期），倒序（最新在前） */
    fun allKeys(drops: List<Drop>, settings: AppSettings): List<String> {
        val keys = drops.map { of(it.date, settings).key }.toMutableSet()
        keys.add(of(LocalDate.now(), settings).key)
        return keys.sortedDescending()
    }

    data class Info(
        val index: Int,
        val key: String,
        val start: LocalDate,
        val end: LocalDate,
        val nextStart: LocalDate,
        val daysLeft: Int,
        val label: String
    )
}
