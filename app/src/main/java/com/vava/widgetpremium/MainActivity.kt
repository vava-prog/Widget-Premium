package com.vava.widgetpremium

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import android.os.Build
import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    /** Đổi biểu tượng ngày/đêm khi rời ứng dụng, tránh đổi lúc đang mở. */
    override fun onStop() {
        super.onStop()
        LauncherIcon.apply(this, WidgetUpdater.isDay(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (WeatherStore.city(this) != null) WeatherScheduler.schedule(this)
        DayNightScheduler.scheduleNext(this)
        val startTab = intent?.getIntExtra(EXTRA_TAB, 0) ?: 0
        setContent {
            WidgetPremiumTheme {
                AppRoot(startTab)
            }
        }
    }

    companion object {
        const val EXTRA_TAB = "open_tab"
    }
}

@Composable
private fun AppRoot(startTab: Int) {
    var tab by remember { mutableStateOf(startTab) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0, onClick = { tab = 0 },
                    icon = { Text("🏠") }, label = { Text(stringResource(R.string.nav_home)) }
                )
                NavigationBarItem(
                    selected = tab == 1, onClick = { tab = 1 },
                    icon = { Text("📅") }, label = { Text(stringResource(R.string.nav_calendar)) }
                )
            }
        }
    ) { padding ->
        Surface(Modifier.fillMaxSize().padding(padding), color = MaterialTheme.colorScheme.background) {
            if (tab == 0) HomeScreen()
            else CalendarScreen(Modifier.verticalScroll(rememberScrollState()))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeScreen() {
    val context = LocalContext.current
    val steps = listOf(R.string.guide_step1, R.string.guide_step2, R.string.guide_step3)
    var style by remember { mutableStateOf(StyleStore.load(context)) }
    val onStyleChange: (WidgetStyle) -> Unit = { new ->
        style = new.sanitized()
        StyleStore.save(context, style)
        WidgetUpdater.updateWidgets(context)
    }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            // Chỉ đệm hai bên: khung xem trước được ghim sát mép trên, không cách 24dp.
            contentPadding = PaddingValues(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Box(Modifier.padding(top = 24.dp)) { Dashboard() } }
            item {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    LiveLogo(Modifier.fillMaxWidth(0.8f))
                }
            }
            item {
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            item { Text(stringResource(R.string.home_intro), style = MaterialTheme.typography.bodyLarge) }
            item {
                Button(
                    onClick = { requestPinWidget(context, ClockWidgetProvider::class.java) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.btn_add_widget)) }
            }
            item {
                Button(
                    onClick = { requestPinWidget(context, InfoWidgetProvider::class.java) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.btn_add_info_widget)) }
            }
            item {
                Button(
                    onClick = { requestPinWidget(context, WeekWidgetProvider::class.java) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.btn_add_week_widget)) }
            }
            item { WeatherSection() }
            item {
                Text(stringResource(R.string.style_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            // Khung xem trước được ghim phía trên khi cuộn qua các thanh chỉnh giao diện.
            stickyHeader {
                Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.background) {
                    Column(Modifier.padding(vertical = 6.dp)) { StylePreview(style) }
                }
            }
            item { StyleSection(style, onStyleChange) }
            // Tiêu đề rỗng kế tiếp thay thế khung xem trước, để nó không che các phần phía dưới.
            stickyHeader { Spacer(Modifier.height(0.dp)) }
            item { IconSwitchRow() }
            item { AlertsRow() }
            item { DiagnosticsCard() }
            item { Text(stringResource(R.string.guide_title), style = MaterialTheme.typography.titleMedium) }
            items(steps.size) { index ->
                Text("${index + 1}. " + stringResource(steps[index]), style = MaterialTheme.typography.bodyMedium)
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun IconSwitchRow() {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(LauncherIcon.isFeatureEnabled(context)) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.icon_switch_title),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )
            Switch(checked = enabled, onCheckedChange = {
                enabled = it
                LauncherIcon.setFeatureEnabled(context, it)
            })
        }
        Text(
            stringResource(R.string.icon_switch_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AlertsRow() {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(DeviceAlerts.isEnabled(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) Toast.makeText(context, R.string.notes_perm_denied, Toast.LENGTH_LONG).show()
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.alert_switch_title), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Switch(checked = enabled, onCheckedChange = {
                enabled = it
                DeviceAlerts.setEnabled(context, it)
                if (it && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !ReminderScheduler.hasNotificationPermission(context)) {
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            })
        }
        Text(
            stringResource(R.string.alert_switch_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Nhờ launcher ghim widget lên màn hình chính (nếu launcher hỗ trợ). */
private fun requestPinWidget(context: Context, provider: Class<*>) {
    val manager = context.getSystemService(AppWidgetManager::class.java)
    if (manager != null && manager.isRequestPinAppWidgetSupported) {
        manager.requestPinAppWidget(ComponentName(context, provider), null, null)
    } else {
        Toast.makeText(context, R.string.add_widget_unsupported, Toast.LENGTH_LONG).show()
    }
}
