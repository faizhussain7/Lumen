package com.mfhapps.lumen

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

data class IconPackInfo(val packageName: String, val label: String)

object IconPacks {
    private val actions = listOf(
        "org.adw.launcher.THEMES",
        "com.novalauncher.THEME",
        "com.gau.go.launcherex.theme",
        "com.teslacoilsw.launcher.THEME",
        "com.anddoes.launcher.THEME",
        "app.lawnchair.icons.THEMED_ICON",
    )
    private var loadedPack: String? = null
    private var mapping: Map<String, String> = emptyMap()
    private var resources: Resources? = null

    fun installed(context: Context): List<IconPackInfo> {
        val pm = context.packageManager
        return actions.flatMap { action ->
            runCatching { pm.queryIntentActivities(Intent(action), PackageManager.GET_META_DATA) }.getOrDefault(emptyList())
        }.map { it.activityInfo.packageName }.distinct().map { pkg ->
            IconPackInfo(pkg, runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg))
        }.sortedBy { it.label.lowercase() }
    }

    @Synchronized
    fun drawable(context: Context, pack: String, component: ComponentName): Drawable? {
        if (pack.isBlank()) return null
        if (pack != loadedPack) load(context, pack)
        val res = resources ?: return null
        val name = mapping["ComponentInfo{${component.packageName}/${component.className}}"] ?: return null
        val id = res.getIdentifier(name, "drawable", pack).takeIf { it != 0 } ?: return null
        return runCatching { res.getDrawable(id, null) }.getOrNull()
    }

    private fun load(context: Context, pack: String) {
        loadedPack = pack
        mapping = emptyMap()
        resources = runCatching { context.packageManager.getResourcesForApplication(pack) }.getOrNull() ?: return
        val res = resources!!
        val parser: XmlPullParser? = runCatching {
            val xmlId = res.getIdentifier("appfilter", "xml", pack)
            if (xmlId != 0) res.getXml(xmlId) else XmlPullParserFactory.newInstance().newPullParser().apply { setInput(res.assets.open("appfilter.xml"), "utf-8") }
        }.getOrNull()
        parser ?: return
        val out = HashMap<String, String>()
        runCatching {
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && parser.name == "item") {
                    val component = parser.getAttributeValue(null, "component")
                    val drawable = parser.getAttributeValue(null, "drawable")
                    if (component != null && drawable != null) out[component] = drawable
                }
                event = parser.next()
            }
        }
        mapping = out
    }
}
