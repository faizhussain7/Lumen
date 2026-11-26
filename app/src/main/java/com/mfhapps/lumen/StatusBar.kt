package com.mfhapps.lumen

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SignalCellular0Bar
import androidx.compose.material.icons.rounded.SignalCellularAlt1Bar
import androidx.compose.material.icons.rounded.SignalCellularAlt2Bar
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.NetworkWifi1Bar
import androidx.compose.material.icons.rounded.NetworkWifi2Bar
import androidx.compose.material.icons.rounded.NetworkWifi3Bar
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.Bolt
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.telephony.TelephonyManager
import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun LumenStatusBar(prefs: LumenPrefs, ink: Color, modifier: Modifier = Modifier, onNotices: () -> Unit = {}) {
    val context = LocalContext.current
    val now = rememberNow()
    val (level, charging) = rememberBattery()
    var network by remember { mutableStateOf("") }
    var bars by remember { mutableIntStateOf(0) }
    var wifi by remember { mutableStateOf(false) }
    var wifiLevel by remember { mutableIntStateOf(0) }
    val badges by Badges.state.collectAsState()
    LaunchedEffect(Unit) {
        while (true) {
            val cm = context.getSystemService(ConnectivityManager::class.java)
            val caps = runCatching { cm.getNetworkCapabilities(cm.activeNetwork) }.getOrNull()
            wifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            if (wifi) {
                val wm = context.getSystemService(android.net.wifi.WifiManager::class.java)
                @Suppress("DEPRECATION")
                val rssi = runCatching { wm.connectionInfo.rssi }.getOrDefault(-70)
                wifiLevel = runCatching { wm.calculateSignalLevel(rssi) * 3 / wm.maxSignalLevel.coerceAtLeast(1) }.getOrDefault(2)
            }
            val tm = context.getSystemService(TelephonyManager::class.java)
            network = runCatching { tm.networkOperatorName }.getOrNull().orEmpty()
            bars = runCatching { tm.signalStrength?.level ?: 0 }.getOrDefault(0)
            delay(4000)
        }
    }
    val total = badges.values.sum()
    val scheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = modifier.fillMaxWidth().height(28.dp).padding(horizontal = 22.dp),
    ) {
        Text(DateFormat.format(if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm", now).toString(), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = ink)
        if (total > 0) {
            Spacer(Modifier.width(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.tap(CircleShape, depth = 0.1f, onClick = onNotices).background(scheme.primary).padding(start = 5.dp, end = 7.dp, top = 1.dp, bottom = 1.dp),
            ) {
                GlyphIcon(Glyph.Bell, scheme.onPrimary, 12.dp)
                Spacer(Modifier.width(3.dp))
                Text("$total", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = scheme.onPrimary)
            }
        }
        Spacer(Modifier.weight(1f))
        if (network.isNotEmpty()) Text(network, style = MaterialTheme.typography.labelMedium, color = ink.copy(alpha = 0.85f))
        androidx.compose.material3.Icon(
            when (bars) {
                0 -> Icons.Rounded.SignalCellular0Bar
                1 -> Icons.Rounded.SignalCellularAlt1Bar
                2 -> Icons.Rounded.SignalCellularAlt2Bar
                else -> Icons.Rounded.SignalCellularAlt
            },
            contentDescription = null,
            tint = ink,
            modifier = Modifier.size(17.dp),
        )
        if (wifi) {
            androidx.compose.material3.Icon(
                when (wifiLevel) {
                    0 -> Icons.Rounded.NetworkWifi1Bar
                    1 -> Icons.Rounded.NetworkWifi2Bar
                    2 -> Icons.Rounded.NetworkWifi3Bar
                    else -> Icons.Rounded.Wifi
                },
                contentDescription = null,
                tint = ink,
                modifier = Modifier.size(17.dp),
            )
        }
        if (level >= 0) BatteryWave(level, charging, ink, 18.dp, showPercent = prefs.statusPercentState.value)
    }
}

@Composable
fun BatteryIcon(level: Int, charging: Boolean, ink: Color, showPercent: Boolean = false, height: androidx.compose.ui.unit.Dp = 13.dp) {
    val good = Color(0xFF34C759)
    val low = Color(0xFFFF453A)
    val fill = when {
        charging -> good
        level <= 15 -> low
        else -> ink
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (showPercent) {
            Text("$level%", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = ink)
            Spacer(Modifier.width(4.dp))
        }
        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(width = height * 2f, height = height)) {
                val tip = 2.2.dp.toPx()
                val body = Size(size.width - tip - 1.dp.toPx(), size.height)
                val radius = CornerRadius(size.height * 0.32f)
                drawRoundRect(ink.copy(alpha = 0.32f), size = body, cornerRadius = radius)
                val inset = 1.6.dp.toPx()
                val inner = Size((body.width - inset * 2f) * (level.coerceIn(0, 100) / 100f), body.height - inset * 2f)
                if (inner.width > 0f) drawRoundRect(fill, topLeft = Offset(inset, inset), size = inner, cornerRadius = CornerRadius(size.height * 0.22f))
                drawRoundRect(ink.copy(alpha = 0.5f), topLeft = Offset(body.width + 1.dp.toPx(), size.height * 0.32f), size = Size(tip, size.height * 0.36f), cornerRadius = CornerRadius(tip))
            }
            if (charging) {
                androidx.compose.material3.Icon(
                    Icons.Rounded.Bolt,
                    contentDescription = null,
                    tint = if (level > 55) Color.Black.copy(alpha = 0.75f) else Color.White,
                    modifier = Modifier.size(height).padding(end = 2.dp),
                )
            }
        }
    }
}
