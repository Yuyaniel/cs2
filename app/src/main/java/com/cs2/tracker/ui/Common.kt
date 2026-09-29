package com.cs2.tracker.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cs2.tracker.data.CategoryMeta
import com.cs2.tracker.data.Drop
import com.cs2.tracker.data.money

/** 品质色条 */
@Composable
fun RarityDot(colorHex: String?, modifier: Modifier = Modifier) {
    val c = runCatching { Color(android.graphics.Color.parseColor(colorHex ?: "#6B7280")) }
        .getOrDefault(TextFaint)
    Box(modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(c))
}

/** 无水波纹点击 */
@Composable
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    return this.clickable(interactionSource = interaction, indication = null, onClick = onClick)
}

/** 物品图片（OkHttp 加载 Steam CDN；无图/失败回退 emoji） */
@Composable
fun ItemImg(
    url: String?, emoji: String, modifier: Modifier = Modifier,
    bg: Color = Color(0xFF22262E), contentScale: ContentScale = ContentScale.Fit
) {
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = url) {
        value = if (url.isNullOrBlank()) null else ImageLoader.load(url)
    }
    Box(
        modifier.clip(RoundedCornerShape(12.dp)).background(bg),
        contentAlignment = Alignment.Center
    ) {
        val b = bitmap
        if (b != null) {
            Image(
                bitmap = b.asImageBitmap(), contentDescription = null,
                contentScale = contentScale, modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(emoji, fontSize = 22.sp)
        }
    }
}

/** 期徽章：当前期橙色，往期灰 */
@Composable
fun PeriodBadge(index: Int, isCurrent: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (isCurrent) Cs2Orange else Color(0xFF262A33)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "第 $index 期",
            color = if (isCurrent) Color(0xFF1A1A1A) else TextDim,
            fontSize = 12.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

/** 掉落物格子（快捷卡片样式，横排网格用） */
@Composable
fun DropCell(drop: Drop, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF22262E))
            .clickableNoRipple(onClick)
            .padding(horizontal = 6.dp, vertical = 10.dp)
            .width(80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ItemImg(drop.img, CategoryMeta.emojis[drop.category] ?: "🎁", Modifier.size(46.dp))
        Spacer(Modifier.height(5.dp))
        Text(
            drop.name, fontSize = 11.sp, color = Color(0xFFE8EAED), textAlign = TextAlign.Center,
            maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 13.sp
        )
        val total = drop.price * drop.qty
        val sub = buildList {
            if (drop.qty > 1) add("×${drop.qty}")
            if (drop.price != 0.0) add("¥${money(total)}")
        }.joinToString(" · ")
        if (sub.isNotEmpty()) {
            Text(
                sub, fontSize = 10.sp,
                color = if (drop.price < 0) Cs2Red else TextFaint,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Text(drop.date, fontSize = 9.sp, color = TextFaint)
    }
}
