package com.vava.widgetpremium

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WidgetPremiumTheme {
                HomeScreen()
            }
        }
    }
}

@Composable
private fun HomeScreen() {
    val context = LocalContext.current
    val steps = listOf(R.string.guide_step1, R.string.guide_step2, R.string.guide_step3)
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(stringResource(R.string.home_intro), style = MaterialTheme.typography.bodyLarge)
            Button(
                onClick = { requestPinWidget(context, ClockWidgetProvider::class.java) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.btn_add_widget))
            }
            Button(
                onClick = { requestPinWidget(context, InfoWidgetProvider::class.java) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.btn_add_info_widget))
            }
            Text(stringResource(R.string.guide_title), style = MaterialTheme.typography.titleMedium)
            steps.forEachIndexed { index, res ->
                Text("${index + 1}. " + stringResource(res), style = MaterialTheme.typography.bodyMedium)
            }
        }
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
