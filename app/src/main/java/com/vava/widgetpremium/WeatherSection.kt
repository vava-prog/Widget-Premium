package com.vava.widgetpremium

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.io.IOException
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException

@Composable
fun WeatherSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<City>>(emptyList()) }
    var city by remember { mutableStateOf(WeatherStore.city(context)) }
    var weather by remember { mutableStateOf(WeatherStore.cached(context)) }
    var busy by remember { mutableStateOf(false) }
    var messageRes by remember { mutableStateOf<Int?>(null) }

    fun refreshWeather() {
        scope.launch {
            busy = true
            val ok = WeatherRepository.refresh(context)
            weather = WeatherStore.cached(context)
            messageRes = if (ok) null else R.string.weather_offline
            busy = false
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.weather_title), style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(stringResource(R.string.weather_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                scope.launch {
                    busy = true
                    messageRes = null
                    try {
                        val found = withContext(Dispatchers.IO) { WeatherApi.searchCities(query) }
                        results = found
                        if (found.isEmpty()) messageRes = R.string.weather_no_result
                    } catch (e: IOException) {
                        messageRes = R.string.weather_search_failed
                    } catch (e: JSONException) {
                        messageRes = R.string.weather_search_failed
                    }
                    busy = false
                }
            },
            enabled = query.isNotBlank() && !busy,
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.btn_search)) }

        results.forEach { candidate ->
            TextButton(
                onClick = {
                    WeatherStore.saveCity(context, candidate)
                    city = candidate
                    weather = null
                    results = emptyList()
                    WeatherScheduler.schedule(context)
                    refreshWeather()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(candidate.label) }
        }

        messageRes?.let {
            Text(stringResource(it), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }

        val savedCity = city
        if (savedCity != null) {
            Text(savedCity.label, style = MaterialTheme.typography.titleSmall)
            val w = weather
            if (w != null) {
                val condition = Condition.fromWmo(w.code)
                Text(
                    stringResource(
                        R.string.weather_current_fmt,
                        condition.emoji,
                        w.tempC.roundToInt().toString(),
                        stringResource(condition.labelRes()),
                        w.humidity
                    ),
                    style = MaterialTheme.typography.bodyLarge
                )
                val fetched = WeatherStore.fetchedAtMs(context)
                if (fetched > 0) {
                    Text(
                        stringResource(R.string.weather_updated_fmt, Formats.clockTime(fetched)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (w.days.isNotEmpty()) {
                    Text(stringResource(R.string.weather_forecast_title), style = MaterialTheme.typography.titleSmall)
                    w.days.forEach { d ->
                        Text(
                            Formats.shortDate(d.date) + "   " + Condition.fromWmo(d.code).emoji + "   " +
                                d.minC.roundToInt() + "° / " + d.maxC.roundToInt() + "°",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            Button(onClick = { refreshWeather() }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.btn_refresh))
            }
        }

        Text(
            stringResource(R.string.weather_privacy),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            stringResource(R.string.weather_attribution),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun Condition.labelRes(): Int = when (this) {
    Condition.CLEAR -> R.string.cond_clear
    Condition.PARTLY -> R.string.cond_partly
    Condition.CLOUDY -> R.string.cond_cloudy
    Condition.FOG -> R.string.cond_fog
    Condition.DRIZZLE -> R.string.cond_drizzle
    Condition.RAIN -> R.string.cond_rain
    Condition.SNOW -> R.string.cond_snow
    Condition.THUNDER -> R.string.cond_thunder
    Condition.UNKNOWN -> R.string.cond_unknown
}
