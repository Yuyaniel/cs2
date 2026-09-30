package com.cs2.tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.cs2.tracker.data.CatalogItem
import com.cs2.tracker.data.CsgoApi
import com.cs2.tracker.data.Drop
import com.cs2.tracker.data.DropStore
import com.cs2.tracker.ui.AddDropSheet
import com.cs2.tracker.ui.Cs2Orange
import com.cs2.tracker.ui.Cs2Theme
import com.cs2.tracker.ui.HomeScreen
import com.cs2.tracker.ui.ImageLoader
import com.cs2.tracker.ui.SearchScreen
import com.cs2.tracker.ui.SettingsScreen
import com.cs2.tracker.ui.StatsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        DropStore.init(this)
        CsgoApi.init(this)
        ImageLoader.init(this)

        setContent {
            Cs2Theme { App() }
        }
    }
}

private data class Tab(val label: String, val icon: ImageVector)

@Composable
private fun App() {
    val tabs = listOf(
        Tab("首页", Icons.Default.DateRange),
        Tab("统计", Icons.Default.Star),
        Tab("添加", Icons.Default.Add),
        Tab("设置", Icons.Default.Settings)
    )
    var tab by remember { mutableIntStateOf(0) }
    var addOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Drop?>(null) }
    var prefill by remember { mutableStateOf<CatalogItem?>(null) }
    var jumpKey by remember { mutableStateOf<String?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val toast by DropStore.toast.collectAsState()
    toast?.let { msg ->
        LaunchedEffect(msg) {
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
            DropStore.consumeToast()
        }
    }

    Scaffold(
        containerColor = Color(0xFF14161A),
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF1B1E24)) {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Cs2Orange,
                            selectedTextColor = Cs2Orange,
                            indicatorColor = Cs2Orange.copy(alpha = 0.15f),
                            unselectedIconColor = Color(0xFF9AA1AD),
                            unselectedTextColor = Color(0xFF9AA1AD)
                        )
                    )
                }
            }
        }
    ) { padding ->
        androidx.compose.foundation.layout.Box(Modifier.padding(padding)) {
            when (tab) {
                0 -> HomeScreen(
                    onEditDrop = { editing = it; addOpen = true },
                    jumpKey = jumpKey,
                    onJumpConsumed = { jumpKey = null }
                )
                1 -> StatsScreen()
                2 -> SearchScreen(
                    onJumpToPeriod = { key ->
                        jumpKey = key
                        tab = 0
                    },
                    onAddItem = { item ->
                        prefill = item
                        editing = null
                        addOpen = true
                    }
                )
                3 -> SettingsScreen()
            }
        }
    }

    if (addOpen) {
        AddDropSheet(
            editing = editing,
            prefill = prefill,
            onDismiss = { addOpen = false; editing = null; prefill = null }
        )
    }
}
