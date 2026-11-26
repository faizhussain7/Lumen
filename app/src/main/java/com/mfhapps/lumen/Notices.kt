package com.mfhapps.lumen

import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

class NoticeGroup(val pkg: String, val app: String, val items: List<Notice>)

fun groupNotices(list: List<Notice>): List<NoticeGroup> =
    list.groupBy { it.pkg }.map { (pkg, items) -> NoticeGroup(pkg, items.first().app, items.sortedByDescending { it.time }) }.sortedByDescending { it.items.first().time }

fun LazyListScope.noticeGroups(groups: List<NoticeGroup>, openGroups: MutableState<Set<String>>, onOpen: (PendingIntent) -> Unit) {
    groups.forEach { group ->
        if (group.items.size == 1) {
            item(key = group.items.first().key) { SwipeNotice(group.items.first(), onOpen, Modifier.padding(vertical = 1.5.dp).stagger(groups.indexOf(group))) }
        } else {
            val open = group.pkg in openGroups.value
            item(key = "group:" + group.pkg) {
                Box(Modifier.stagger(groups.indexOf(group))) { GroupHeader(group, open) { openGroups.value = if (open) openGroups.value - group.pkg else openGroups.value + group.pkg } }
            }
            if (open) group.items.forEachIndexed { i, notice ->
                item(key = notice.key) { SwipeNotice(notice, onOpen, Modifier.padding(start = 18.dp, top = 1.5.dp, bottom = 1.5.dp).stagger(i)) }
            }
        }
    }
}

@Composable
private fun GroupHeader(group: NoticeGroup, open: Boolean, onToggle: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 1.5.dp).fillMaxWidth().listTap(rest = 10.dp, onClick = onToggle).background(scheme.surfaceContainerHigh.copy(alpha = 0.92f)).padding(12.dp),
    ) {
        AppBadge(group.pkg, 36.dp)
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(group.app, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface, maxLines = 1)
            if (!open) Text(group.items.first().title + if (group.items.first().text.isNotBlank()) ": " + group.items.first().text else "", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 1)
        }
        Text("${group.items.size}", style = flexStyle(13f, 800, 100, scheme.onPrimary), modifier = Modifier.clip(CircleShape).background(scheme.primary).padding(horizontal = 9.dp, vertical = 3.dp))
        androidx.compose.material3.Icon(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.padding(start = 6.dp))
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SwipeNotice(notice: Notice, onOpen: (PendingIntent) -> Unit, modifier: Modifier = Modifier) {
    val springs = LocalSprings.current
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    var open by remember { mutableStateOf(false) }
    var replyTo by remember { mutableStateOf<NoticeAction?>(null) }
    var reply by remember { mutableStateOf("") }
    Box(modifier.fillMaxWidth()) {
        Column(
            Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .graphicsLayer { alpha = 1f - (abs(offset.value) / (size.width.coerceAtLeast(1f))).coerceIn(0f, 0.8f) }
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(scheme.surfaceContainerHigh.copy(alpha = 0.92f))
                .pointerInput(notice.key) {
                    if (!notice.clearable) return@pointerInput
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dx -> change.consume(); scope.launch { offset.snapTo(offset.value + dx) } },
                        onDragEnd = {
                            val limit = size.width * 0.35f
                            if (abs(offset.value) > limit) {
                                haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                                scope.launch {
                                    offset.animateTo(if (offset.value > 0) size.width * 1.2f else -size.width * 1.2f)
                                    LumenNotifications.dismiss(notice.key)
                                }
                            } else {
                                scope.launch { offset.animateTo(0f, springs.fastSpatial()) }
                            }
                        },
                    )
                }
                .clickable { open = !open }
                .padding(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppBadge(notice.pkg, 36.dp)
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(notice.app + " · " + android.text.format.DateFormat.format("h:mm", notice.time), style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant, maxLines = 1)
                    Text(notice.title, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface, maxLines = 1)
                    if (!open && notice.text.isNotBlank()) Text(notice.text, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 2)
                }
                androidx.compose.material3.Icon(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = null, tint = scheme.onSurfaceVariant)
            }
            if (open) {
                if (notice.text.isNotBlank()) Text(notice.text, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface, maxLines = 12, modifier = Modifier.padding(start = 48.dp, top = 8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(start = 48.dp, top = 10.dp)) {
                    notice.intent?.let { intent -> FabPill(stringResource(R.string.notice_open), Glyph.Home, emphasis = true) { onOpen(intent) } }
                    notice.actions.forEach { action ->
                        FabPill(action.title, if (action.input != null) Glyph.Data else null) {
                            if (action.input != null) replyTo = action else runCatching { action.intent.send() }
                        }
                    }
                    FabPill(stringResource(R.string.notice_snooze), Glyph.Alarm) { LumenNotifications.snooze(notice.key, 60 * 60_000L) }
                    if (notice.clearable) FabPill(stringResource(R.string.notice_dismiss), Glyph.Delete) { LumenNotifications.dismiss(notice.key) }
                    FabPill(stringResource(R.string.notice_settings), Glyph.Gear) { openNoticeSettings(context, notice.pkg) }
                }
                replyTo?.let { action ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 48.dp, top = 10.dp).fillMaxWidth().height(52.dp).clip(CircleShape).background(scheme.surfaceContainerHighest).padding(start = 18.dp, end = 6.dp),
                    ) {
                        Box(Modifier.weight(1f)) {
                            if (reply.isEmpty()) Text(action.title, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurfaceVariant)
                            BasicTextField(reply, { reply = it }, singleLine = true, textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface), cursorBrush = SolidColor(scheme.primary), modifier = Modifier.fillMaxWidth())
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(42.dp).tap(CircleShape, depth = 0.12f) {
                                if (reply.isNotBlank()) {
                                    sendReply(context, action, reply)
                                    reply = ""
                                    replyTo = null
                                }
                            }.background(scheme.primary),
                        ) { androidx.compose.material3.Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, tint = scheme.onPrimary, modifier = Modifier.size(20.dp)) }
                    }
                }
            }
        }
    }
}

private fun sendReply(context: Context, action: NoticeAction, text: String) {
    val input = action.input ?: return
    val intent = Intent()
    RemoteInput.addResultsToIntent(arrayOf(input), intent, Bundle().apply { putCharSequence(input.resultKey, text) })
    runCatching { action.intent.send(context, 0, intent) }
}

fun openNoticeSettings(context: Context, pkg: String) {
    runCatching {
        context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

@Composable
fun NoticeCards(notices: List<Notice>, onOpen: (PendingIntent) -> Unit, modifier: Modifier = Modifier, limit: Int = 8) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = modifier.clip(RoundedCornerShape(26.dp))) {
        notices.take(limit).forEach { SwipeNotice(it, onOpen) }
    }
}

@Composable
fun AppBadge(pkg: String, size: Dp) {
    val context = LocalContext.current
    val px = with(LocalDensity.current) { size.roundToPx() }
    val bitmap = remember(pkg) { runCatching { context.packageManager.getApplicationIcon(pkg).toBitmap(px, px).asImageBitmap() }.getOrNull() }
    Box(Modifier.size(size).clip(CircleShape), contentAlignment = Alignment.Center) {
        bitmap?.let { androidx.compose.foundation.Image(it, contentDescription = null, modifier = Modifier.size(size)) }
    }
}

fun sendNotice(context: Context, intent: PendingIntent) {
    runCatching {
        val options = android.app.ActivityOptions.makeBasic()
        if (android.os.Build.VERSION.SDK_INT >= 34) options.setPendingIntentBackgroundActivityStartMode(android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
        intent.send(context, 0, null, null, null, null, options.toBundle())
    }
}
