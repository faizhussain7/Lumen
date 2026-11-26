package com.mfhapps.lumen

import android.Manifest
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class WeatherNow(val temperature: Int, val code: Int, val day: Boolean)

enum class WeatherStatus { Idle, Ok, NoPermission, LocationOff, NoFix, Offline }

class Place(val name: String, val detail: String, val lat: Double, val lon: Double)

object Weather {
    val state = MutableStateFlow<WeatherNow?>(null)
    val status = MutableStateFlow(WeatherStatus.Idle)
    private var fetchedAt = 0L

    fun permitted(context: Context) = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun prefs(context: Context) = context.getSharedPreferences("lumen", Context.MODE_PRIVATE)

    fun manualPlace(context: Context): Place? {
        val p = prefs(context)
        val name = p.getString("weather_city", null) ?: return null
        return Place(name, "", p.getFloat("weather_lat", 0f).toDouble(), p.getFloat("weather_lon", 0f).toDouble())
    }

    fun setManualPlace(context: Context, place: Place?) {
        val e = prefs(context).edit()
        if (place == null) e.remove("weather_city").remove("weather_lat").remove("weather_lon")
        else e.putString("weather_city", place.name).putFloat("weather_lat", place.lat.toFloat()).putFloat("weather_lon", place.lon.toFloat())
        e.apply()
        fetchedAt = 0L
    }

    suspend fun searchPlaces(query: String): List<Place> = withContext(Dispatchers.IO) {
        if (query.trim().length < 2) return@withContext emptyList()
        runCatching {
            val url = URL("https://geocoding-api.open-meteo.com/v1/search?count=6&language=" + Locale.getDefault().language + "&name=" + java.net.URLEncoder.encode(query.trim(), "UTF-8"))
            val body = (url.openConnection() as HttpURLConnection).apply { connectTimeout = 8000; readTimeout = 8000 }.inputStream.bufferedReader().use { it.readText() }
            val results = JSONObject(body).optJSONArray("results") ?: return@runCatching emptyList()
            (0 until results.length()).map { i ->
                val o = results.getJSONObject(i)
                Place(o.getString("name"), listOfNotNull(o.optString("admin1").ifBlank { null }, o.optString("country").ifBlank { null }).joinToString(", "), o.getDouble("latitude"), o.getDouble("longitude"))
            }
        }.getOrDefault(emptyList())
    }

    @android.annotation.SuppressLint("MissingPermission")
    private suspend fun deviceLocation(context: Context): Pair<Double, Double>? {
        val lm = context.getSystemService(LocationManager::class.java)
        val known = listOf(LocationManager.FUSED_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER, LocationManager.GPS_PROVIDER)
            .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
        if (known != null) return known.latitude to known.longitude
        val provider = listOf(LocationManager.FUSED_PROVIDER, LocationManager.NETWORK_PROVIDER).firstOrNull { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) } ?: return null
        return kotlinx.coroutines.withTimeoutOrNull(12_000) {
            kotlinx.coroutines.suspendCancellableCoroutine { cont ->
                val signal = android.os.CancellationSignal()
                cont.invokeOnCancellation { signal.cancel() }
                runCatching {
                    lm.getCurrentLocation(provider, signal, context.mainExecutor) { loc -> if (cont.isActive) cont.resumeWith(Result.success(loc?.let { it.latitude to it.longitude })) }
                }.onFailure { if (cont.isActive) cont.resumeWith(Result.success(null)) }
            }
        }
    }

    suspend fun refresh(context: Context, force: Boolean = false) {
        if (!force && System.currentTimeMillis() - fetchedAt < 30 * 60_000L && state.value != null) return
        val manual = manualPlace(context)
        val coords: Pair<Double, Double> = when {
            manual != null -> manual.lat to manual.lon
            !permitted(context) -> { status.value = WeatherStatus.NoPermission; return }
            !context.getSystemService(LocationManager::class.java).isLocationEnabled -> { status.value = WeatherStatus.LocationOff; return }
            else -> deviceLocation(context) ?: run { status.value = WeatherStatus.NoFix; return }
        }
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val lat = "%.1f".format(Locale.US, coords.first)
                val lon = "%.1f".format(Locale.US, coords.second)
                val fahrenheit = Locale.getDefault().country in setOf("US", "LR", "MM")
                val url = URL("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code,is_day" + if (fahrenheit) "&temperature_unit=fahrenheit" else "")
                val connection = (url.openConnection() as HttpURLConnection).apply { connectTimeout = 8000; readTimeout = 8000 }
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val current = JSONObject(body).getJSONObject("current")
                WeatherNow(Math.round(current.getDouble("temperature_2m")).toInt(), current.getInt("weather_code"), current.optInt("is_day", 1) == 1)
            }.getOrNull()
        }
        if (result != null) {
            state.value = result
            status.value = WeatherStatus.Ok
            fetchedAt = System.currentTimeMillis()
        } else {
            status.value = WeatherStatus.Offline
        }
    }
}

@Composable
fun rememberWeather(enabled: Boolean): WeatherNow? {
    val context = LocalContext.current
    val now by Weather.state.collectAsState()
    LaunchedEffect(enabled) {
        while (enabled) {
            Weather.refresh(context)
            delay(if (Weather.status.value == WeatherStatus.Ok) 10 * 60_000L else 60_000L)
        }
    }
    return if (enabled) now else null
}

fun weatherIcon(w: WeatherNow): ImageVector = when (w.code) {
    0, 1 -> if (w.day) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay
    2, 3, 45, 48 -> Icons.Rounded.Cloud
    in 51..67, in 80..82 -> Icons.Rounded.WaterDrop
    in 71..77, 85, 86 -> Icons.Rounded.AcUnit
    in 95..99 -> Icons.Rounded.Thunderstorm
    else -> Icons.Rounded.Cloud
}

@Composable
fun weatherLabel(w: WeatherNow): String = stringResource(
    when (w.code) {
        0 -> R.string.weather_clear
        1, 2 -> R.string.weather_partly
        3 -> R.string.weather_cloudy
        45, 48 -> R.string.weather_fog
        in 51..67, in 80..82 -> R.string.weather_rain
        in 71..77, 85, 86 -> R.string.weather_snow
        in 95..99 -> R.string.weather_storm
        else -> R.string.weather_cloudy
    },
)

@Composable
fun WeatherWidget(size: Dp, weather: WeatherNow?) {
    val scheme = MaterialTheme.colorScheme
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size).clip(androidx.compose.foundation.shape.CircleShape).background(scheme.tertiaryContainer)) {
        if (weather == null) {
            androidx.compose.material3.Icon(Icons.Rounded.Cloud, contentDescription = null, tint = scheme.onTertiaryContainer.copy(alpha = 0.6f), modifier = Modifier.size(size * 0.4f))
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                androidx.compose.material3.Icon(weatherIcon(weather), contentDescription = null, tint = scheme.tertiary, modifier = Modifier.size(size * 0.26f))
                Text("${weather.temperature}°", style = flexStyle(size.value * 0.34f, 850, 70, scheme.onTertiaryContainer))
            }
        }
    }
}
