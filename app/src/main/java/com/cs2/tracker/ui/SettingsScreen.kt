package com.cs2.tracker.ui

import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cs2.tracker.data.AppSettings
import com.cs2.tracker.data.CsgoApi
import com.cs2.tracker.data.DropStore
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val data by DropStore.data.collectAsState()

    var startDate by remember(data.settings) { mutableStateOf(data.settings.periodStart) }
    var daysText by remember(data.settings) { mutableStateOf(data.settings.periodDays.toString()) }
    var showPicker by remember { mutableStateOf(false) }
    val apiState by CsgoApi.state.collectAsState()

    val dpState = rememberDatePickerState(
        initialSelectedDateMillis = runCatching {
            java.time.ZoneId.systemDefault()
            LocalDate.parse(startDate).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }.getOrElse { System.currentTimeMillis() }
    )

    // 导出
    val exportJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { os ->
                os.write(DropStore.exportJson().toByteArray())
            }
            DropStore.toast("已导出 JSON 备份")
        }
    }
    val exportCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { os ->
                os.write(DropStore.exportCsv().toByteArray(charset("UTF-8")))
            }
            DropStore.toast("已导出 CSV")
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val text = context.contentResolver.openInputStream(it)?.use { s ->
                s.readBytes().toString(Charsets.UTF_8)
            } ?: return@let
            DropStore.toast(DropStore.importJson(text))
        }
    }

    if (showPicker) {
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dpState.selectedDateMillis?.let {
                        startDate = Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate().toString()
                    }
                    showPicker = false
                }) { Text("确定", color = Cs2Orange) }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("取消", color = TextDim) } },
            colors = DatePickerDefaults.colors(containerColor = CardColor)
        ) { DatePicker(state = dpState, colors = DatePickerDefaults.colors(containerColor = CardColor)) }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 周期设置
        SectionTitle("周期设置")
        Column(
            Modifier.clip(RoundedCornerShape(14.dp)).background(CardColor).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                Text("第 1 期起始：$startDate", color = Color(0xFFE8EAED))
            }
            OutlinedTextField(
                value = daysText,
                onValueChange = { daysText = it.filter(Char::isDigit) },
                label = { Text("每期天数", fontSize = 13.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    val s = AppSettings(
                        periodStart = startDate,
                        periodDays = daysText.toIntOrNull()?.coerceAtLeast(1) ?: 7
                    )
                    DropStore.saveSettings(s)
                    DropStore.toast("周期已保存")
                },
                colors = ButtonDefaults.buttonColors(containerColor = Cs2Orange, contentColor = Color(0xFF1A1A1A)),
                modifier = Modifier.fillMaxWidth()
            ) { Text("保存设置", fontWeight = FontWeight.Bold) }
        }

        // 数据管理
        SectionTitle("数据管理")
        Column(
            Modifier.clip(RoundedCornerShape(14.dp)).background(CardColor).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        exportJson.launch("cs2-drops-backup.json")
                    }, modifier = Modifier.weight(1f)
                ) { Text("导出备份", fontSize = 12.sp) }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json")) },
                    modifier = Modifier.weight(1f)
                ) { Text("导入备份", fontSize = 12.sp) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { exportCsv.launch("cs2-drops.csv") },
                    modifier = Modifier.weight(1f)
                ) { Text("导出 CSV", fontSize = 12.sp) }
                var showClear by remember { mutableStateOf(false) }
                OutlinedButton(
                    onClick = { showClear = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Cs2Red),
                    modifier = Modifier.weight(1f)
                ) { Text("清空数据", fontSize = 12.sp) }
                if (showClear) {
                    AlertDialog(
                        onDismissRequest = { showClear = false },
                        containerColor = CardColor,
                        titleContentColor = TextMain,
                        textContentColor = TextDim,
                        title = { Text("清空全部掉落记录？", fontSize = 16.sp) },
                        text = { Text("此操作不可恢复，建议先导出备份。", fontSize = 13.sp) },
                        confirmButton = {
                            TextButton(onClick = {
                                DropStore.clearAll()
                                showClear = false
                                DropStore.toast("已清空")
                            }) { Text("清空", color = Cs2Red) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showClear = false }) { Text("取消", color = TextDim) }
                        }
                    )
                }
            }
            Text(
                "备份格式与 Web 版互通；导入按 id 去重合并",
                fontSize = 11.sp, color = TextFaint
            )
        }

        // 物品目录（CSGO-API）
        SectionTitle("物品目录（ByMykel/CSGO-API）")
        Column(
            Modifier.clip(RoundedCornerShape(14.dp)).background(CardColor).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (val st = apiState) {
                is CsgoApi.State.Ready -> {
                    Text(
                        "已缓存 ${st.items.size} 个物品（${DateFormat.format("yyyy-MM-dd HH:mm", java.util.Date(st.at))}）",
                        fontSize = 12.sp, color = TextDim
                    )
                    OutlinedButton(onClick = { scope.launch { CsgoApi.refresh() } }, modifier = Modifier.fillMaxWidth()) {
                        Text("更新目录", fontSize = 12.sp)
                    }
                }
                is CsgoApi.State.Loading -> Text("加载中：${st.step}…", fontSize = 12.sp, color = TextDim)
                is CsgoApi.State.Error -> {
                    Text("加载失败：${st.msg}", fontSize = 12.sp, color = Cs2Red)
                    OutlinedButton(onClick = { scope.launch { CsgoApi.refresh() } }, modifier = Modifier.fillMaxWidth()) {
                        Text("重试", fontSize = 12.sp)
                    }
                }
                is CsgoApi.State.NotLoaded -> {
                    Text("未加载（添加物品/搜索时需要）", fontSize = 12.sp, color = TextDim)
                    Button(
                        onClick = { scope.launch { CsgoApi.refresh() } },
                        colors = ButtonDefaults.buttonColors(containerColor = Cs2Orange, contentColor = Color(0xFF1A1A1A)),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("加载目录（约 16MB，仅一次）", fontSize = 12.sp) }
                }
            }
            Text("数据来源：github.com/ByMykel/CSGO-API（zh-CN），图片为 Steam CDN 直链", fontSize = 11.sp, color = TextFaint)
        }
    }
}

@Composable
private fun SectionTitle(t: String) {
    Text(t, fontSize = 13.sp, color = TextDim)
}
