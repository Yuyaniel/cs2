package com.cs2.tracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cs2.tracker.data.*
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun HomeScreen(
    onEditDrop: (Drop) -> Unit,
    jumpKey: String?,
    onJumpConsumed: () -> Unit
) {
    val data by DropStore.data.collectAsState()
    val settings = data.settings
    val drops = data.drops
    val cur = remember(settings) { Period.of(LocalDate.now(), settings) }

    val listState = rememberLazyListState()
    val periodKeys = remember(drops, settings) { Period.allKeys(drops, settings) }

    // 搜索页跳转定位
    LaunchedEffect(jumpKey) {
        if (jumpKey != null) {
            val idx = periodKeys.indexOf(jumpKey)
            if (idx >= 0) listState.animateScrollToItem(idx + 2) // +2: header(统计+日历)
            onJumpConsumed()
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item(key = "top") {
            TopStatsBar(drops, cur)
        }
        item(key = "calendar") {
            CalendarCard(settings)
            Spacer(Modifier.height(12.dp))
        }
        itemsIndexed(periodKeys, key = { _, k -> k }) { _, key ->
            PeriodCard(key, drops, settings, isCurrent = key == cur.key, onEditDrop = onEditDrop)
        }
    }
}

@Composable
private fun TopStatsBar(drops: List<Drop>, cur: Period.Info) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCell(
            "${cur.daysLeft}天", "距下期",
            Modifier.weight(1f)
        )
        val total = drops.sumOf { it.price * it.qty }
        StatCell("¥${money(total)}", "总收益/支出", Modifier.weight(1f), accent = true)
    }
}

@Composable
private fun StatCell(num: String, label: String, modifier: Modifier = Modifier, accent: Boolean = false) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CardColor)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            num, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
            color = if (accent) Cs2Orange else Color(0xFFE8EAED),
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        Text(label, fontSize = 11.sp, color = TextFaint)
    }
}

/** 仿手机日历：本期胶囊高亮 + 今天圆形 */
@Composable
private fun CalendarCard(settings: AppSettings) {
    var month by remember { mutableStateOf(YearMonth.now()) }
    val cur = remember(settings) { Period.of(LocalDate.now(), settings) }
    val today = remember { LocalDate.now() }

    Column(
        Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(listOf(Color(0xFF242938), CardColor2))
            )
            .padding(14.dp)
    ) {
        // 头部：‹ 9月 2026 ›
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleNavBtn("‹") { month = month.minusMonths(1) }
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${month.monthValue}月", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.width(6.dp))
                Text("${month.year}", fontSize = 11.sp, color = TextFaint)
            }
            CircleNavBtn("›") { month = month.plusMonths(1) }
        }
        Spacer(Modifier.height(10.dp))
        // 星期表头
        Row(Modifier.fillMaxWidth()) {
            listOf("日", "一", "二", "三", "四", "五", "六").forEach {
                Text(
                    it, Modifier.weight(1f), fontSize = 11.sp,
                    color = TextFaint, textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        // 日期网格
        val firstDay = month.atDay(1).dayOfWeek.value % 7
        val daysInMonth = month.lengthOfMonth()
        val cells = remember(month) {
            (0 until firstDay).map { null } + (1..daysInMonth).map { month.atDay(it) }
        }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                week.forEachIndexed { i, d ->
                    val cellMod = Modifier.weight(1f)
                    if (d == null) {
                        Spacer(cellMod.height(30.dp))
                    } else {
                        val inPeriod = !d.isBefore(cur.start) && !d.isAfter(cur.end)
                        val isToday = d == today
                        val leftIn = i > 0 && week[i - 1]?.let {
                            !it.isBefore(cur.start) && !it.isAfter(cur.end)
                        } == true
                        val rightIn = i < 6 && week.getOrNull(i + 1)?.let {
                            !it.isBefore(cur.start) && !it.isAfter(cur.end)
                        } == true
                        val shape = when {
                            !inPeriod -> RoundedCornerShape(999.dp)
                            !leftIn && !rightIn -> CircleShape
                            !leftIn -> RoundedCornerShape(topStart = 999.dp, bottomStart = 999.dp)
                            !rightIn -> RoundedCornerShape(topEnd = 999.dp, bottomEnd = 999.dp)
                            else -> RoundedCornerShape(0.dp)
                        }
                        Box(
                            cellMod
                                .height(30.dp)
                                .clip(shape)
                                .background(if (inPeriod) Cs2Orange.copy(alpha = 0.16f) else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isToday) {
                                Box(
                                    Modifier.fillMaxSize().padding(2.dp).clip(CircleShape)
                                        .background(Cs2Orange)
                                )
                            }
                            Text(
                                "${d.dayOfMonth}", fontSize = 13.sp,
                                color = when {
                                    isToday -> Color(0xFF1A1A1A)
                                    inPeriod -> Cs2Orange
                                    else -> Color(0xFFC8CCD4)
                                },
                                fontWeight = if (inPeriod || isToday) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
                // 补齐行尾空位
                repeat(7 - week.size) { Spacer(Modifier.weight(1f).height(30.dp)) }
            }
        }
    }
}

@Composable
private fun CircleNavBtn(symbol: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color(0xFF262A33))
            .clickableNoRipple(onClick),
        contentAlignment = Alignment.Center
    ) { Text(symbol, fontSize = 16.sp, color = Color(0xFFE8EAED)) }
}

/** 单期卡片框 */
@Composable
private fun PeriodCard(
    key: String,
    drops: List<Drop>,
    settings: AppSettings,
    isCurrent: Boolean,
    onEditDrop: (Drop) -> Unit
) {
    val p = remember(key, settings) { Period.of(key, settings) }
    val items = remember(drops, key) { drops.filter { Period.of(it.date, settings).key == key } }
    val value = items.sumOf { it.price * it.qty }
    val count = items.sumOf { it.qty }

    Column(
        Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CardColor)
            .padding(12.dp)
    ) {
        if (!isCurrent) Spacer(Modifier.height(2.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                PeriodBadge(p.index, isCurrent)
                Spacer(Modifier.height(4.dp))
                Text(p.label, fontSize = 11.sp, color = TextDim)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "¥${money(value)}", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold,
                    color = Cs2Orange
                )
                Text("$count 件", fontSize = 11.sp, color = TextDim)
            }
        }
        Spacer(Modifier.height(10.dp))
        if (items.isEmpty()) {
            Text(
                if (isCurrent) "本期还没有记录，点 + 打卡" else "本期无记录",
                fontSize = 12.sp, color = TextFaint,
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        } else {
            FlowRowGrid(items, onEditDrop)
        }
    }
}

@Composable
fun FlowRowGrid(items: List<Drop>, onEditDrop: (Drop) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(items, key = { _, d -> d.id }) { _, d ->
            DropCell(drop = d, onClick = { onEditDrop(d) })
        }
    }
}
