package com.cs2.tracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.cs2.tracker.data.*
import java.time.LocalDate

@Composable
fun StatsScreen() {
    val data by DropStore.data.collectAsState()
    val drops = data.drops
    val settings = data.settings

    val totalValue = drops.sumOf { it.price * it.qty }
    val totalQty = drops.sumOf { it.qty }
    val best = drops.maxByOrNull { it.price }

    // 武器箱次数
    val caseStats = remember(drops) {
        drops.filter { it.category == "case" }
            .groupBy { it.name }
            .map { (name, list) -> name to Triple(list.sumOf { it.qty }, list.sumOf { it.price * it.qty }, list.firstOrNull()?.img) }
            .sortedByDescending { it.second.first }
    }
    // 按类型/品质
    val byCat = remember(drops) {
        drops.groupBy { it.category }.map { (k, v) -> k to v.sumOf { it.price * it.qty } }.sortedByDescending { it.second }
    }
    val byRar = remember(drops) {
        drops.groupBy { it.rarity }.map { (k, v) -> k to v.sumOf { it.price * it.qty } }.sortedByDescending { it.second }
    }
    // 各期（按年）
    val years = remember(drops) {
        (drops.map { Period.of(it.date, settings).start.year }.toSet() + LocalDate.now().year).sortedDescending()
    }
    var year by remember { mutableIntStateOf(LocalDate.now().year) }
    val periodStats = remember(drops, settings, year) {
        drops.map { Period.of(it.date, settings) to it }
            .filter { it.first.start.year == year }
            .groupBy { it.first.key }
            .toSortedMap(compareByDescending { it })
            .map { (k, list) -> Period.of(k, settings) to list.map { it.second } }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 总览
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardColor)
                    .border(1.dp, LineColor, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text("累计总收益/支出", fontSize = 12.sp, color = TextDim)
                Text(
                    "¥${money(totalValue)}",
                    fontSize = 34.sp, fontWeight = FontWeight.ExtraBold,
                    color = if (totalValue < 0) Cs2Red else Cs2Orange
                )
                Text(
                    "累计 $totalQty 件 · 最贵掉落 ${best?.name ?: "-"}",
                    fontSize = 12.sp, color = TextDim
                )
            }
        }
        // 武器箱掉落次数
        item {
            Column(
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardColor2)
                    .border(1.dp, LineColor, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Text("武器箱掉落次数", fontSize = 13.sp, color = TextDim)
                Spacer(Modifier.height(10.dp))
                if (caseStats.isEmpty()) {
                    Text("暂无武器箱记录", fontSize = 12.sp, color = TextFaint, modifier = Modifier.padding(8.dp))
                }
                caseStats.forEach { (name, s) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CardColor)
                            .border(1.dp, LineColor, RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ItemImg(s.third, "📦", Modifier.size(40.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("¥${money(s.second)}", fontSize = 11.sp, color = TextDim)
                        }
                        Text("${s.first} 次", color = Cs2Orange, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    }
                }
            }
        }
        // 按类型
        item { BarBlock("按类型", byCat, { CategoryMeta.labels[it] ?: it }) }
        // 各期价值（方格）
        item {
            var selectedPeriod by remember { mutableStateOf<Pair<Period.Info, List<Drop>>?>(null) }
            Column(
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardColor2)
                    .border(1.dp, LineColor, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("各期收益/支出", fontSize = 13.sp, color = TextDim)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(years, key = { _, y -> y }) { _, y ->
                            FilterChip(
                                selected = y == year,
                                onClick = { year = y },
                                label = { Text("${y}年", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Cs2Orange,
                                    selectedLabelColor = Color(0xFF1A1A1A)
                                )
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (periodStats.isEmpty()) {
                    Text("$year 年暂无记录", fontSize = 12.sp, color = TextFaint, modifier = Modifier.padding(8.dp))
                }
                val rows = periodStats.chunked(3)
                rows.forEach { rowItems ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowItems.forEach { (p, list) ->
                            PeriodGridCell(
                                p = p, list = list,
                                modifier = Modifier.weight(1f),
                                onClick = { selectedPeriod = p to list }
                            )
                        }
                        repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            PeriodDetailDialog(selectedPeriod) { selectedPeriod = null }
        }
    }
}

@Composable
private fun PeriodGridCell(p: Period.Info, list: List<Drop>, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val qty = list.sumOf { it.qty }
    val value = list.sumOf { it.price * it.qty }
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CardColor)
            .border(1.dp, LineColor, RoundedCornerShape(12.dp))
            .clickableNoRipple(onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("第 ${p.index} 期", fontSize = 13.sp, color = TextMain, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(p.label, fontSize = 8.sp, color = TextFaint, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        Text(
            "¥${money(value)}", fontSize = 13.sp,
            color = if (value < 0) Cs2Red else Cs2Orange,
            fontWeight = FontWeight.Bold, maxLines = 1
        )
        Text("${qty} 件", fontSize = 10.sp, color = TextFaint)
    }
}

@Composable
private fun PeriodDetailDialog(pair: Pair<Period.Info, List<Drop>>?, onDismiss: () -> Unit) {
    if (pair == null) return
    val (p, list) = pair
    val sorted = remember(pair) { list.sortedByDescending { it.date } }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.82f),
            shape = RoundedCornerShape(20.dp),
            color = CardColor2
        ) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Text(
                    "第 ${p.index} 期 · ${p.label}",
                    fontSize = 16.sp, color = TextMain, fontWeight = FontWeight.Bold
                )
                Text(
                    "${sorted.sumOf { it.qty }} 件 · ¥${money(sorted.sumOf { it.price * it.qty })}",
                    fontSize = 13.sp, color = TextDim
                )
                Spacer(Modifier.height(12.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(sorted, key = { _, d -> d.id }) { _, d ->
                        DropRow(d)
                    }
                }
            }
        }
    }
}

@Composable
private fun DropRow(d: Drop) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardColor)
            .border(1.dp, LineColor, RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ItemImg(d.img, CategoryMeta.emojis[d.category] ?: "🎁", Modifier.size(44.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(d.name, fontSize = 13.sp, color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(d.date, fontSize = 11.sp, color = TextFaint)
        }
        Text(
            "×${d.qty} · ¥${money(d.price * d.qty)}",
            fontSize = 12.sp, color = if (d.price < 0) Cs2Red else Cs2Orange,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun BarBlock(title: String, entries: List<Pair<String, Double>>, label: (String) -> String) {
    Column(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CardColor2)
            .border(1.dp, LineColor, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Text(title, fontSize = 13.sp, color = TextDim)
        Spacer(Modifier.height(10.dp))
        val max = entries.maxOfOrNull { it.second }?.takeIf { it > 0 } ?: 1.0
        val total = entries.sumOf { it.second }
        if (entries.isEmpty()) {
            Text("暂无数据", fontSize = 12.sp, color = TextFaint, modifier = Modifier.padding(8.dp))
        }
        entries.forEach { (k, v) ->
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label(k), fontSize = 12.sp, color = TextDim, modifier = Modifier.width(56.dp))
                Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF22262E))) {
                    Box(
                        Modifier.fillMaxHeight().fillMaxWidth((v / max).toFloat())
                            .clip(RoundedCornerShape(4.dp)).background(Cs2Orange)
                    )
                }
                Text(
                    "¥${money(v)} · ${if (total > 0) (v / total * 100).toInt() else 0}%",
                    fontSize = 11.sp, color = TextDim,
                    modifier = Modifier.width(110.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
            }
        }
    }
}
