package com.mfhapps.lumen

import android.Manifest
import android.app.SearchManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.provider.ContactsContract
import android.provider.AlarmClock
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private class Contact(val id: Long, val name: String, val phone: String?)

class QuickAction(val title: String, val glyph: Glyph, val run: (android.content.Context) -> Unit)

private fun quickActions(query: String, context: android.content.Context): List<QuickAction> {
    val q = query.trim()
    val lower = q.lowercase()
    val out = ArrayList<QuickAction>()
    Regex("""^(timer|countdown)\s+(\d+)\s*(s|sec|secs|seconds|m|min|mins|minutes|h|hr|hours)?$""").find(lower)?.let { m ->
        val n = m.groupValues[2].toInt()
        val secs = when (m.groupValues[3].firstOrNull()) { 's' -> n; 'h' -> n * 3600; else -> n * 60 }
        out += QuickAction(context.getString(R.string.qa_timer, m.groupValues[2] + " " + (m.groupValues[3].ifBlank { "min" })), Glyph.Alarm) {
            it.startActivity(Intent(AlarmClock.ACTION_SET_TIMER).putExtra(AlarmClock.EXTRA_LENGTH, secs).putExtra(AlarmClock.EXTRA_SKIP_UI, false).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
    Regex("""^(alarm|wake me)\s+(?:at\s+)?(\d{1,2})(?::(\d{2}))?\s*(am|pm)?$""").find(lower)?.let { m ->
        var h = m.groupValues[2].toInt(); val min = m.groupValues[3].toIntOrNull() ?: 0
        if (m.groupValues[4] == "pm" && h < 12) h += 12
        if (m.groupValues[4] == "am" && h == 12) h = 0
        out += QuickAction(context.getString(R.string.qa_alarm, "%d:%02d".format(h, min)), Glyph.Alarm) {
            it.startActivity(Intent(AlarmClock.ACTION_SET_ALARM).putExtra(AlarmClock.EXTRA_HOUR, h).putExtra(AlarmClock.EXTRA_MINUTES, min).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
    Regex("""^(navigate to|directions to|maps?)\s+(.+)$""").find(lower)?.let { m ->
        val place = q.substring(q.length - m.groupValues[2].length)
        out += QuickAction(context.getString(R.string.qa_navigate, place), Glyph.Location) {
            it.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("geo:0,0?q=" + android.net.Uri.encode(place))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
    Regex("""^(youtube|yt)\s+(.+)$""").find(lower)?.let { m ->
        val what = q.substring(q.length - m.groupValues[2].length)
        out += QuickAction(context.getString(R.string.qa_youtube, what), Glyph.Volume) {
            it.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.youtube.com/results?search_query=" + android.net.Uri.encode(what))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
    Units.convert(lower)?.let { result -> out += QuickAction(result, Glyph.Calculate) { ctx -> copy(ctx, result.substringAfter("= ").trim()) } }
    return out
}

private fun copy(context: android.content.Context, text: String) {
    context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("result", text))
    Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
}

object Units {
    private val table = mapOf(
        "km" to ("length" to 1000.0), "m" to ("length" to 1.0), "cm" to ("length" to 0.01), "mm" to ("length" to 0.001),
        "mi" to ("length" to 1609.344), "miles" to ("length" to 1609.344), "mile" to ("length" to 1609.344), "ft" to ("length" to 0.3048), "feet" to ("length" to 0.3048), "in" to ("length" to 0.0254), "inch" to ("length" to 0.0254), "inches" to ("length" to 0.0254), "yd" to ("length" to 0.9144),
        "kg" to ("mass" to 1.0), "g" to ("mass" to 0.001), "lb" to ("mass" to 0.45359237), "lbs" to ("mass" to 0.45359237), "oz" to ("mass" to 0.028349523),
        "l" to ("volume" to 1.0), "ml" to ("volume" to 0.001), "gal" to ("volume" to 3.785411784),
        "kmh" to ("speed" to 1 / 3.6), "mph" to ("speed" to 0.44704), "ms" to ("speed" to 1.0),
    )

    fun convert(text: String): String? {
        val m = Regex("""^([\d.]+)\s*([a-z°]+)\s+(?:in|to)\s+([a-z°]+)$""").find(text.replace("km/h", "kmh")) ?: return null
        val v = m.groupValues[1].toDoubleOrNull() ?: return null
        val from = m.groupValues[2]; val to = m.groupValues[3]
        val temps = setOf("c", "°c", "f", "°f", "k")
        if (from in temps && to in temps) {
            val c = when (from.trimStart('°')) { "f" -> (v - 32) * 5 / 9; "k" -> v - 273.15; else -> v }
            val r = when (to.trimStart('°')) { "f" -> c * 9 / 5 + 32; "k" -> c + 273.15; else -> c }
            return "${fmt(v)} ${from.uppercase()} = ${fmt(r)} ${to.uppercase()}"
        }
        val a = table[from] ?: return null; val b = table[to] ?: return null
        if (a.first != b.first) return null
        return "${fmt(v)} $from = ${fmt(v * a.second / b.second)} $to"
    }

    private fun fmt(d: Double) = if (d == Math.floor(d) && kotlin.math.abs(d) < 1e12) d.toLong().toString() else "%.4f".format(d).trimEnd('0').trimEnd('.')
}

@Composable
fun SearchResults(
    query: String,
    apps: List<AppEntry>,
    catalog: AppCatalog,
    prefs: LumenPrefs,
    iconSize: Dp,
    onLaunch: (AppEntry, Rect?) -> Unit,
    onMenu: (AppEntry, Rect?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) }
    val askContacts = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    val calc = remember(query) { Calculator.evaluate(query) }
    val actions = remember(query) { quickActions(query, context) }
    val settings = remember(query) { SettingsIndex.search(query) }
    val shortcuts by produceState(emptyList<Pair<AppEntry, android.content.pm.ShortcutInfo>>(), query, apps) {
        value = if (query.trim().length < 2) emptyList() else withContext(Dispatchers.IO) { catalog.searchShortcuts(query.trim()).take(5) }
    }
    val contacts by produceState(emptyList<Contact>(), query, granted) {
        value = if (granted && query.trim().length >= 2) withContext(Dispatchers.IO) { findContacts(context, query.trim().removePrefix("call ").removePrefix("message ").trim()) } else emptyList()
    }
    val saveRecent = { prefs.addRecent(query.trim()) }
    var index = 0
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val top = apps.firstOrNull()
        if (top != null) {
            item(key = "top") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.stagger(index++).fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(scheme.primaryContainer).padding(14.dp),
                ) {
                    AppIcon(top, catalog, prefs, 56.dp, showLabel = false, labelColor = scheme.onPrimaryContainer, onLaunch = { saveRecent(); onLaunch(top, it) }, onMenu = { onMenu(top, it) })
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.search_top), style = MaterialTheme.typography.labelMedium, color = scheme.onPrimaryContainer.copy(alpha = 0.75f))
                        Text(top.label, style = flexStyle(24f, 850, 100, scheme.onPrimaryContainer), maxLines = 1)
                    }
                    FabPill(stringResource(R.string.notice_open), null, emphasis = true) { saveRecent(); onLaunch(top, null) }
                }
            }
        }
        if (calc != null) {
            item(key = "calc") {
                Column(
                    Modifier.stagger(index++).fillMaxWidth().tap(RoundedCornerShape(28.dp), depth = 0.03f) { copy(context, calc) }.background(scheme.tertiaryContainer).padding(20.dp),
                ) {
                    Text(stringResource(R.string.search_section_calc), style = MaterialTheme.typography.labelLarge, color = scheme.onTertiaryContainer.copy(alpha = 0.75f))
                    Text("= $calc", style = flexStyle(34f, 900, 100, scheme.onTertiaryContainer))
                }
            }
        }
        if (actions.isNotEmpty()) {
            item(key = "actions") {
                ResultGroup(Modifier.stagger(index++)) { actions.forEach { a -> ResultRow(a.title, null, a.glyph) { saveRecent(); runCatching { a.run(context) } } } }
            }
        }
        if (apps.size > 1) {
            item(key = "apps-label") { SectionLabel(stringResource(R.string.search_section_apps)) }
            item(key = "apps") {
                Column(Modifier.stagger(index++).fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(scheme.surfaceContainerHigh.copy(alpha = 0.7f)).padding(vertical = 14.dp)) {
                    apps.drop(1).take(prefs.columns.coerceAtLeast(4) * 2).chunked(prefs.columns.coerceAtLeast(4)).forEach { row ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            row.forEach { app ->
                                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                    AppIcon(app, catalog, prefs, iconSize, showLabel = true, labelColor = scheme.onSurface, onLaunch = { saveRecent(); onLaunch(app, it) }, onMenu = { onMenu(app, it) })
                                }
                            }
                            repeat(prefs.columns.coerceAtLeast(4) - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
        if (shortcuts.isNotEmpty()) {
            item(key = "sc-label") { SectionLabel(stringResource(R.string.shortcuts)) }
            item(key = "sc") {
                ResultGroup(Modifier.stagger(index++)) {
                    shortcuts.forEach { (app, sc) ->
                        ResultRow((sc.shortLabel ?: sc.longLabel ?: "").toString() + " · " + app.label, null, null, badge = app.packageName) { saveRecent(); catalog.startShortcut(sc, null) }
                    }
                }
            }
        }
        if (settings.isNotEmpty()) {
            item(key = "set-label") { SectionLabel(stringResource(R.string.search_section_settings)) }
            item(key = "set") {
                ResultGroup(Modifier.stagger(index++)) {
                    settings.forEach { e -> ResultRow(e.title, null, Glyph.Gear) { saveRecent(); openSetting(context, e.action, query) } }
                }
            }
        }
        if (granted && contacts.isNotEmpty()) {
            item(key = "con-label") { SectionLabel(stringResource(R.string.search_section_contacts)) }
            item(key = "con") {
                ResultGroup(Modifier.stagger(index++)) {
                    contacts.forEach { c ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().listTap {
                            saveRecent()
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, c.id)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                        }.background(scheme.surfaceContainerHigh.copy(alpha = 0.85f)).padding(horizontal = 16.dp, vertical = 10.dp)) {
                            Box(Modifier.size(40.dp).clip(PolygonShape(LumenShapes.clockBadge)).background(scheme.tertiaryContainer), contentAlignment = Alignment.Center) {
                                Text(c.name.take(1).uppercase(), style = MaterialTheme.typography.titleMedium, color = scheme.onTertiaryContainer)
                            }
                            Spacer(Modifier.width(14.dp))
                            Text(c.name, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface, maxLines = 1, modifier = Modifier.weight(1f))
                            if (c.phone != null) {
                                IconBubble(Glyph.Data) { runCatching { context.startActivity(Intent(Intent.ACTION_SENDTO, android.net.Uri.parse("smsto:" + c.phone)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
                                Spacer(Modifier.width(8.dp))
                                IconBubble(Glyph.Phone) { runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:" + c.phone)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
                            }
                        }
                    }
                }
            }
        }
        item(key = "more") {
            ResultGroup(Modifier.stagger(index++)) {
                ResultRow(stringResource(R.string.search_web, query.trim()), null, null, search = true) {
                    saveRecent()
                    runCatching { context.startActivity(Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, query.trim()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }
                ResultRow(stringResource(R.string.search_in_settings, query.trim()), null, Glyph.Gear) { saveRecent(); openSetting(context, "com.android.settings.action.SETTINGS_SEARCH", query) }
                if (apps.isEmpty()) ResultRow(stringResource(R.string.search_play, query.trim()), null, Glyph.Widgets) {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("market://search?q=" + android.net.Uri.encode(query.trim()))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }
                if (!granted) ResultRow(stringResource(R.string.search_contacts_allow), null, Glyph.Person) { askContacts.launch(Manifest.permission.READ_CONTACTS) }
            }
        }
    }
}

private fun openSetting(context: android.content.Context, action: String, query: String) {
    val intent = Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (action == "com.android.settings.action.SETTINGS_SEARCH") intent.putExtra(SearchManager.QUERY, query.trim()).putExtra("query", query.trim())
    runCatching { context.startActivity(intent) }.onFailure { runCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
}

@Composable
private fun ResultGroup(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)), verticalArrangement = Arrangement.spacedBy(2.dp)) { content() }
}

@Composable
private fun IconBubble(glyph: Glyph, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(Modifier.size(40.dp).tap(CircleShape, depth = 0.12f, onClick = onClick).background(scheme.primaryContainer), contentAlignment = Alignment.Center) {
        GlyphIcon(glyph, scheme.onPrimaryContainer, 20.dp)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 10.dp, top = 6.dp),
    )
}

@Composable
private fun ResultRow(title: String, initial: String?, glyph: Glyph?, search: Boolean = false, badge: String? = null, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().listTap(onClick = onClick).background(scheme.surfaceContainerHigh.copy(alpha = 0.85f)).padding(horizontal = 16.dp, vertical = 12.dp)) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(scheme.surfaceContainerHighest), contentAlignment = Alignment.Center) {
            when {
                badge != null -> AppBadge(badge, 40.dp)
                initial != null -> Text(initial, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
                search -> SearchGlyph(scheme.onSurface)
                glyph != null -> GlyphIcon(glyph, scheme.onSurface, 20.dp)
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface, maxLines = 1, modifier = Modifier.weight(1f))
    }
}

private fun findContacts(context: android.content.Context, query: String): List<Contact> = runCatching {
    if (query.isBlank()) return emptyList()
    context.contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        arrayOf(ContactsContract.CommonDataKinds.Phone.CONTACT_ID, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY, ContactsContract.CommonDataKinds.Phone.NUMBER),
        "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} LIKE ?",
        arrayOf("%$query%"),
        "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} ASC",
    )?.use { cursor ->
        val out = LinkedHashMap<Long, Contact>()
        while (cursor.moveToNext() && out.size < 5) {
            val id = cursor.getLong(0)
            if (id !in out) out[id] = Contact(id, cursor.getString(1) ?: continue, cursor.getString(2))
        }
        out.values.toList()
    } ?: emptyList()
}.getOrDefault(emptyList())

object Calculator {
    fun evaluate(input: String): String? {
        val text = input.replace(" ", "").replace('×', '*').replace('÷', '/').replace(',', '.')
        if (text.length < 3 || text.none { it.isDigit() } || text.none { it in "+-*/^%" }) return null
        if (text.any { !(it.isDigit() || it in "+-*/^%.()") }) return null
        return runCatching {
            val parser = Parser(text)
            val value = parser.expression()
            if (parser.pos != text.length || value.isNaN() || value.isInfinite()) null
            else if (value == Math.floor(value) && kotlin.math.abs(value) < 1e15) value.toLong().toString()
            else "%.8f".format(value).trimEnd('0').trimEnd('.')
        }.getOrNull()
    }

    private class Parser(val s: String) {
        var pos = 0
        fun expression(): Double {
            var v = term()
            while (pos < s.length && (s[pos] == '+' || s[pos] == '-')) { val op = s[pos++]; val r = term(); v = if (op == '+') v + r else v - r }
            return v
        }
        fun term(): Double {
            var v = power()
            while (pos < s.length && (s[pos] == '*' || s[pos] == '/' || s[pos] == '%')) {
                val op = s[pos++]; val r = power()
                v = when (op) { '*' -> v * r; '/' -> v / r; else -> v % r }
            }
            return v
        }
        fun power(): Double {
            val base = unary()
            if (pos < s.length && s[pos] == '^') { pos++; return Math.pow(base, power()) }
            return base
        }
        fun unary(): Double {
            if (pos < s.length && s[pos] == '-') { pos++; return -unary() }
            if (pos < s.length && s[pos] == '(') { pos++; val v = expression(); if (pos < s.length && s[pos] == ')') pos++; return v }
            val start = pos
            while (pos < s.length && (s[pos].isDigit() || s[pos] == '.')) pos++
            return s.substring(start, pos).toDouble()
        }
    }
}
