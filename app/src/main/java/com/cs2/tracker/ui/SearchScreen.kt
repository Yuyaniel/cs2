package com.cs2.tracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.cs2.tracker.data.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 搜索页：
 * 1) 添加：分类筛选（收藏/武器/箱子/涂鸦/挂件/饰品）+ 物品网格 + 收藏 → 点击添加
 * 2) 日期：选日期 → 跳到该日期所在期
 * 3) 期数：输入期号 → 跳转
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onJumpToPeriod: (String) -> Unit,
    onAddItem: (CatalogItem) -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("添加", "日期", "期数")

    Column(Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = tab,
            containerColor = Color.Transparent,
            contentColor = Cs2Orange
        ) {
            tabs.forEachIndexed { i, t ->
                Tab(
                    selected = tab == i, onClick = { tab = i },
                    text = { Text(t, fontWeight = if (tab == i) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }
        when (tab) {
            0 -> AddTab(onAddItem)
            1 -> DateJumpTab(onJumpToPeriod)
            2 -> PeriodJumpTab(onJumpToPeriod)
        }
    }
}

/** Tab0：添加 —— 分类筛选 + 点击打开详情 */
@Composable
private fun AddTab(onAddItem: (CatalogItem) -> Unit) {
    val data by DropStore.data.collectAsState()
    val favorites = data.favorites
    val apiState by CsgoApi.state.collectAsState()
    val scope = rememberCoroutineScope()

    val cats = listOf(
        "收藏" to "fav", "武器" to "skin", "箱子" to "case",
        "涂鸦" to "graffiti", "挂件" to "keychain", "饰品" to "other"
    )
    var sel by remember { mutableStateOf("fav") }
    var subSel by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var selectedItem by remember { mutableStateOf<CatalogItem?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("搜索物品名称…", fontSize = 13.sp) }
        )
        Spacer(Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(cats, key = { it.first }) { (label, cat) ->
                FilterChip(
                    selected = sel == cat,
                    onClick = { sel = cat; subSel = "" },
                    label = { Text(label, fontSize = 13.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Cs2Orange,
                        selectedLabelColor = Color(0xFF1A1A1A),
                        labelColor = TextDim
                    )
                )
            }
        }

        // 武器细分类（手枪/步枪/微型冲锋枪/重型武器/匕首/手套/装备）
        if (sel == "skin" && query.isBlank()) {
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = subSel.isBlank(),
                        onClick = { subSel = "" },
                        label = { Text("全部", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Cs2Orange,
                            selectedLabelColor = Color(0xFF1A1A1A),
                            labelColor = TextDim
                        )
                    )
                }
                items(WeaponCats.list, key = { it }) { sub ->
                    FilterChip(
                        selected = subSel == sub,
                        onClick = { subSel = sub },
                        label = { Text(sub, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Cs2Orange,
                            selectedLabelColor = Color(0xFF1A1A1A),
                            labelColor = TextDim
                        )
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        when (val st = apiState) {
            is CsgoApi.State.NotLoaded -> {
                Text("物品目录未加载", color = TextDim, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { scope.launch { CsgoApi.refresh() } },
                    colors = ButtonDefaults.buttonColors(containerColor = Cs2Orange, contentColor = Color(0xFF1A1A1A))
                ) { Text("加载目录（约 16MB，仅一次）") }
            }
            is CsgoApi.State.Loading -> {
                Text("目录加载中：${st.step}…", color = TextDim, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
            }
            is CsgoApi.State.Error -> {
                Text("加载失败：${st.msg}", color = Cs2Red, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { scope.launch { CsgoApi.refresh() } },
                    colors = ButtonDefaults.buttonColors(containerColor = Cs2Orange, contentColor = Color(0xFF1A1A1A))
                ) { Text("重试") }
            }
            is CsgoApi.State.Ready -> {
                val list = remember(sel, subSel, query, st.items.size, favorites) {
                    when {
                        query.isNotBlank() -> st.items.filter { it.name.contains(query, ignoreCase = true) }.take(200)
                        sel == "fav" -> st.items.filter { favorites.contains(it.id) }
                        sel == "skin" -> st.items.filter { it.cat == "skin" && (subSel.isBlank() || it.subCat == subSel) }
                        else -> st.items.filter { it.cat == sel }
                    }
                }
                if (sel == "fav" && list.isEmpty()) {
                    Text(
                        "还没有收藏的物品。\n去物品分类里点开任意物品，再点 ★ 收藏。",
                        fontSize = 13.sp, color = TextFaint,
                        modifier = Modifier.padding(vertical = 20.dp)
                    )
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(list, key = { it.id }) { item ->
                        ItemCard(item = item, onClick = { selectedItem = item })
                    }
                }
            }
        }
    }

    ItemDetailDialog(
        item = selectedItem,
        drops = data.drops,
        isFav = selectedItem?.let { favorites.contains(it.id) } ?: false,
        onToggleFav = { selectedItem?.let { DropStore.toggleFavorite(it.id) } },
        onAdd = onAddItem,
        onDismiss = { selectedItem = null }
    )
}

/** 物品卡片（网格格）：大图 + 名称 + 类别，点击进详情 */
@Composable
private fun ItemCard(item: CatalogItem, onClick: () -> Unit) {
    Column(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CardColor)
            .border(1.dp, LineColor, RoundedCornerShape(14.dp))
            .clickableNoRipple(onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ItemImg(item.img, CategoryMeta.emojis[item.cat] ?: "🎁", Modifier.size(76.dp))
        Spacer(Modifier.height(6.dp))
        Text(
            item.name, fontSize = 12.sp, color = TextMain,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center, lineHeight = 14.sp
        )
        Spacer(Modifier.height(3.dp))
        Text(
            item.rarityName,
            fontSize = 9.sp, color = TextFaint,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
}

/** 物品详情弹窗：大图、已掉落数量/价值、收藏、添加记录 */
@Composable
private fun ItemDetailDialog(
    item: CatalogItem?,
    drops: List<Drop>,
    isFav: Boolean,
    onToggleFav: () -> Unit,
    onAdd: (CatalogItem) -> Unit,
    onDismiss: () -> Unit
) {
    if (item == null) return
    val stats = remember(item.id, drops.size) {
        drops.filter { it.name == item.name }.let { list ->
            val qty = list.sumOf { it.qty }
            val value = list.sumOf { it.price * it.qty }
            qty to value
        }
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = RoundedCornerShape(20.dp),
            color = CardColor2
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(Modifier.size(140.dp)) {
                    ItemImg(item.img, CategoryMeta.emojis[item.cat] ?: "🎁", Modifier.fillMaxSize())
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    item.name, fontSize = 18.sp, color = TextMain,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${CategoryMeta.labels[item.cat] ?: item.cat} · ${item.rarityName}",
                    fontSize = 13.sp, color = TextDim
                )
                Spacer(Modifier.height(18.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatBox("已掉落", "${stats.first} 件")
                    StatBox("总价值", "¥${money(stats.second)}")
                }

                Spacer(Modifier.height(18.dp))
                IconButton(onClick = onToggleFav) {
                    Icon(
                        Icons.Default.Star, contentDescription = "收藏",
                        tint = if (isFav) Cs2Orange else TextFaint,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Text(
                    if (isFav) "已收藏" else "收藏",
                    fontSize = 12.sp,
                    color = if (isFav) Cs2Orange else TextFaint
                )

                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = { onAdd(item); onDismiss() },
                    colors = ButtonDefaults.buttonColors(containerColor = Cs2Orange, contentColor = Color(0xFF1A1A1A)),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("添加掉落记录", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 24.sp, color = Cs2Orange, fontWeight = FontWeight.Bold)
        Text(label, fontSize = 12.sp, color = TextFaint)
    }
}

/** Tab1：日期跳转 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateJumpTab(onJumpToPeriod: (String) -> Unit) {
    val data by DropStore.data.collectAsState()
    val settings = data.settings
    var picked by remember { mutableStateOf<LocalDate?>(null) }
    var showPicker by remember { mutableStateOf(false) }
    val dpState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())

    if (showPicker) {
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dpState.selectedDateMillis?.let {
                        picked = Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate()
                    }
                    showPicker = false
                }) { Text("确定", color = Cs2Orange) }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("取消", color = TextDim) } },
            colors = DatePickerDefaults.colors(containerColor = CardColor)
        ) { DatePicker(state = dpState, colors = DatePickerDefaults.colors(containerColor = CardColor)) }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("跳转到指定日期所在期", fontSize = 13.sp, color = TextDim)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
            Text(picked?.toString() ?: "选择日期", color = TextMain)
        }
        picked?.let { d ->
            Spacer(Modifier.height(8.dp))
            val p = Period.of(d, settings)
            Text("第 ${p.index} 期 · ${p.label}", fontSize = 12.sp, color = Cs2Orange)
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { picked?.let { onJumpToPeriod(Period.of(it, settings).key) } },
            enabled = picked != null,
            colors = ButtonDefaults.buttonColors(containerColor = Cs2Orange, contentColor = Color(0xFF1A1A1A)),
            modifier = Modifier.fillMaxWidth()
        ) { Text("跳转", fontWeight = FontWeight.Bold) }
    }
}

/** Tab2：期数跳转 */
@Composable
private fun PeriodJumpTab(onJumpToPeriod: (String) -> Unit) {
    val data by DropStore.data.collectAsState()
    val settings = data.settings
    var num by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("跳转到指定期", fontSize = 13.sp, color = TextDim)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = num, onValueChange = { num = it.filter(Char::isDigit) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("输入期号，如 78", fontSize = 13.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                val n = num.toIntOrNull() ?: return@Button
                val start = Period.parse(settings.periodStart).plusDays(((n - 1).toLong()) * settings.periodDays)
                onJumpToPeriod(start.toString())
            },
            enabled = num.toIntOrNull() != null,
            colors = ButtonDefaults.buttonColors(containerColor = Cs2Orange, contentColor = Color(0xFF1A1A1A)),
            modifier = Modifier.fillMaxWidth()
        ) { Text("跳转", fontWeight = FontWeight.Bold) }

        Spacer(Modifier.height(20.dp))
        Text("有记录的期（点击跳转）", fontSize = 12.sp, color = TextDim)
        Spacer(Modifier.height(8.dp))
        val keys = remember(data.drops, settings) { Period.allKeys(data.drops, settings) }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(keys, key = { _, k -> k }) { _, k ->
                val p = Period.of(k, settings)
                val items = data.drops.filter { Period.of(it.date, settings).key == k }
                val v = items.sumOf { it.price * it.qty }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CardColor)
                        .clickableNoRipple { onJumpToPeriod(k) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("第 ${p.index} 期", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(p.label, fontSize = 11.sp, color = TextDim)
                    }
                    Text(
                        "${items.sumOf { it.qty }} 件 · ¥${money(v)}",
                        color = Cs2Orange, fontSize = 12.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
