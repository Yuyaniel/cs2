package com.cs2.tracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.cs2.tracker.data.*
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * 添加/编辑掉落底部弹窗：
 * - 掉落期信息 + 期/日期两级选择（第1期起~今天）
 * - 物品搜索选择（CSGO-API 目录，带预览图）
 * - 数量 / 金额（负数=支出，如开箱花费）/ 备注
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDropSheet(
    editing: Drop?,
    prefill: CatalogItem? = null,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val data by DropStore.data.collectAsState()
    val settings = data.settings

    // ---- 表单状态 ----
    var selectedDate by remember(editing) {
        mutableStateOf(editing?.date ?: LocalDate.now().toString())
    }
    var itemName by remember(editing, prefill) { mutableStateOf(editing?.name ?: prefill?.name ?: "") }
    var itemImg by remember(editing, prefill) { mutableStateOf(editing?.img ?: prefill?.img) }
    var itemCat by remember(editing, prefill) { mutableStateOf(editing?.category ?: prefill?.cat ?: "case") }
    var rarityColor by remember(editing, prefill) { mutableStateOf(editing?.rcolor ?: prefill?.rarityColor) }
    var rarityName by remember(editing, prefill) { mutableStateOf(prefill?.rarityName) }
    var qty by remember(editing) { mutableStateOf((editing?.qty ?: 1).toString()) }
    var price by remember(editing) { mutableStateOf(editing?.price?.takeIf { it != 0.0 }?.let { money(it) } ?: "") }
    var note by remember(editing) { mutableStateOf(editing?.note ?: "") }
    var search by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }

    val apiState by CsgoApi.state.collectAsState()
    LaunchedEffect(Unit) { CsgoApi.ensure() }

    val periodOf = remember(selectedDate, settings) { Period.of(selectedDate, settings) }

    // 日期可选列表：最近 6 期 + 编辑记录所在期（按期分组）
    val periodGroups = remember(settings, editing) {
        val cur = Period.of(LocalDate.now(), settings)
        val days = settings.periodDays
        buildList {
            for (i in 0 until 6) {
                add(Period.of(cur.start.minusDays((i * days).toLong()), settings))
            }
            if (editing != null) {
                val p = Period.of(editing.date, settings)
                if (none { it.key == p.key }) add(p)
            }
        }.sortedByDescending { it.start }
    }
    val curGroup = periodGroups.firstOrNull { it.key == periodOf.key } ?: periodOf
    val dayOptions = remember(curGroup) {
        buildList {
            var d = curGroup.start
            val today = LocalDate.now()
            while (!d.isAfter(curGroup.end) && !d.isAfter(today)) {
                add(d); d = d.plusDays(1)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f).fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp),
            color = CardColor2
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp)
                    .padding(vertical = 20.dp)
            ) {
            Text(
                if (editing == null) "添加记录" else "编辑记录",
                fontSize = 17.sp, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(14.dp))

            // 掉落期信息条
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Cs2OrangeDim)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("第 ${periodOf.index} 期", color = Cs2Orange, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(periodOf.label, fontSize = 12.sp, color = TextDim)
            }
            Spacer(Modifier.height(12.dp))

            // 期选择（横向）
            Text("选择期", fontSize = 12.sp, color = TextDim)
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(periodGroups, key = { it.key }) { p ->
                    val active = p.key == periodOf.key
                    FilterChip(
                        selected = active,
                        onClick = {
                            selectedDate = if (!p.start.isAfter(LocalDate.now())) p.start.toString() else p.start.toString()
                        },
                        label = { Text("第${p.index}期", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Cs2Orange,
                            selectedLabelColor = Color(0xFF1A1A1A)
                        )
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            // 期内日期选择（横向）
            Text("掉落日期", fontSize = 12.sp, color = TextDim)
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(dayOptions, key = { it.toString() }) { d ->
                    val active = d.toString() == selectedDate
                    FilterChip(
                        selected = active,
                        onClick = { selectedDate = d.toString() },
                        label = {
                            Text(
                                "${d.monthValue}/${d.dayOfMonth} 周${"日一二三四五六"[d.dayOfWeek.value % 7]}" +
                                    if (d == LocalDate.now()) " ·今" else "",
                                fontSize = 12.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Cs2Orange,
                            selectedLabelColor = Color(0xFF1A1A1A)
                        )
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            // 物品（搜索选择 / 已选展示）
            if (!searching) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("物品", fontSize = 12.sp, color = TextDim)
                    TextButton(onClick = { searching = true; search = itemName }) {
                        Text(if (itemName.isBlank()) "搜索选择 ›" else "更换 ›", color = Cs2Orange, fontSize = 12.sp)
                    }
                }
                if (itemName.isNotBlank()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF22262E))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ItemImg(itemImg, CategoryMeta.emojis[itemCat] ?: "🎁", Modifier.size(48.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(itemName, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RarityDot(rarityColor)
                                Spacer(Modifier.width(5.dp))
                                Text(rarityName ?: (RarityMeta.labels[editing?.rarity] ?: ""), fontSize = 11.sp, color = TextDim)
                            }
                        }
                    }
                } else {
                    // 快捷武器箱（API 目录就绪时）
                    when (val st = apiState) {
                        is CsgoApi.State.Ready -> {
                            Text("快捷选择", fontSize = 12.sp, color = TextDim)
                            Spacer(Modifier.height(6.dp))
                            val quick = remember(st.items) { CsgoApi.quickCases() }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(quick, key = { it.id }) { it ->
                                    Column(
                                        Modifier
                                            .width(74.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF22262E))
                                            .clickableNoRipple {
                                                itemName = it.name; itemImg = it.img
                                                itemCat = it.cat; rarityColor = it.rarityColor; rarityName = it.rarityName
                                            }
                                            .padding(vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        ItemImg(it.img, "📦", Modifier.size(50.dp))
                                        Text(it.name, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 12.sp)
                                    }
                                }
                            }
                        }
                        is CsgoApi.State.NotLoaded -> {
                            TextButton(onClick = { scope.launch { CsgoApi.refresh() } }) {
                                Text("加载物品目录（首次约 16MB）…", fontSize = 12.sp, color = Cs2Orange)
                            }
                        }
                        is CsgoApi.State.Loading -> Text("目录加载中 ${st.step}", fontSize = 12.sp, color = TextDim)
                        is CsgoApi.State.Error -> Text("目录加载失败：${st.msg}", fontSize = 11.sp, color = Cs2Red)
                    }
                }
            } else {
                // 搜索模式
                OutlinedTextField(
                    value = search, onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("搜索武器箱 / 皮肤 / 涂鸦…", fontSize = 13.sp) },
                    singleLine = true,
                    trailingIcon = { TextButton(onClick = { searching = false }) { Text("取消", fontSize = 12.sp) } }
                )
                Spacer(Modifier.height(8.dp))
                val results = remember(search, apiState) { CsgoApi.search(search) }
                LazyColumn(Modifier.height(180.dp)) {
                    itemsIndexed(results, key = { _, it -> it.id }) { _, it ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickableNoRipple {
                                    itemName = it.name; itemImg = it.img
                                    itemCat = it.cat; rarityColor = it.rarityColor; rarityName = it.rarityName
                                    searching = false
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ItemImg(it.img, "📦", Modifier.size(40.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(it.name, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RarityDot(it.rarityColor); Spacer(Modifier.width(5.dp))
                                    Text("${it.rarityName} · ${CategoryMeta.labels[it.cat]}", fontSize = 11.sp, color = TextDim)
                                }
                            }
                        }
                    }
                    if (results.isEmpty() && search.isNotBlank()) {
                        item {
                            Text(
                                "无结果（也可直接手输名称保存）", fontSize = 12.sp, color = TextFaint,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // 数量 + 金额
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("数量", fontSize = 12.sp, color = TextDim)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { qty = ((qty.toIntOrNull() ?: 1) - 1).coerceAtLeast(1).toString() }, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(44.dp)) {
                            Text("−", fontSize = 18.sp)
                        }
                        OutlinedTextField(
                            value = qty, onValueChange = { qty = it.filter(Char::isDigit).ifEmpty { "1" } },
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                            singleLine = true
                        )
                        OutlinedButton(onClick = { qty = ((qty.toIntOrNull() ?: 1) + 1).toString() }, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(44.dp)) {
                            Text("＋", fontSize = 16.sp)
                        }
                    }
                }
                Column(Modifier.weight(1.2f)) {
                    Text("金额 ¥（负数=支出）", fontSize = 12.sp, color = TextDim)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = price, onValueChange = { price = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        placeholder = { Text("0.00", fontSize = 13.sp) }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = note, onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("备注（可选）", fontSize = 13.sp) }
            )
            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (editing != null) {
                    OutlinedButton(
                        onClick = { DropStore.delete(editing.id); onDismiss() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Cs2Red),
                        modifier = Modifier.weight(0.6f)
                    ) { Text("删除") }
                }
                Button(
                    onClick = {
                        if (itemName.isBlank()) { DropStore.toast("请先选择或输入物品名称"); return@Button }
                        val q = qty.toIntOrNull()?.coerceAtLeast(1) ?: 1
                        val pr = price.toDoubleOrNull() ?: 0.0
                        val drop = Drop(
                            id = editing?.id ?: DropStore.newId(),
                            name = itemName, date = selectedDate, qty = q, price = pr,
                            note = note.trim(), category = itemCat,
                            rarity = editing?.rarity ?: "base",
                            img = itemImg, rcolor = rarityColor
                        )
                        if (editing == null) DropStore.add(drop) else DropStore.update(drop)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Cs2Orange, contentColor = Color(0xFF1A1A1A)),
                    modifier = Modifier.weight(1f)
                ) { Text(if (editing == null) "保存" else "更新", fontWeight = FontWeight.Bold) }
            }
            }
        }
    }
}
