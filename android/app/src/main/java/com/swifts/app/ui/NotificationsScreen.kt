package com.swifts.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.swifts.app.R
import com.swifts.app.domain.AppNotification
import com.swifts.app.ui.theme.LocalSwiftsColors

@Composable
fun NotificationsScreen(vm: NotificationsViewModel, onBack: () -> Unit, onOpenProfile: (String) -> Unit) {
    val state by vm.state.collectAsState()
    val colors = LocalSwiftsColors.current

    LaunchedEffect(Unit) { vm.markReadSoon() }

    val newItems = state.items.filter { !it.read }
    val earlier = state.items.filter { it.read }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            "← " + stringResource(R.string.back),
            style = MaterialTheme.typography.labelLarge,
            color = colors.accent,
            modifier = Modifier.clickable(onClick = onBack).padding(vertical = 8.dp),
        )
        Text(
            stringResource(R.string.notifications_title),
            style = MaterialTheme.typography.headlineSmall,
            color = colors.primaryText,
        )

        LazyColumn {
            if (newItems.isNotEmpty()) {
                item { SectionLabel(stringResource(R.string.section_new)) }
                items(newItems, key = { it.id }) { n ->
                    NotificationRow(n, highlighted = true, onClick = { n.actor_id?.let(onOpenProfile) })
                }
            }
            if (earlier.isNotEmpty()) {
                item { SectionLabel(stringResource(R.string.section_earlier)) }
                items(earlier, key = { it.id }) { n ->
                    NotificationRow(n, highlighted = false, onClick = { n.actor_id?.let(onOpenProfile) })
                }
            }
            if (state.items.isEmpty() && !state.loading) {
                item {
                    Text(
                        stringResource(R.string.no_notifications),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.mutedText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(n: AppNotification, highlighted: Boolean, onClick: () -> Unit) {
    val colors = LocalSwiftsColors.current
    val who = n.actor?.let { it.display_name.ifBlank { it.username } } ?: stringResource(R.string.someone)
    val template = when (n.kind) {
        "like" -> R.string.notif_like
        "follow" -> R.string.notif_follow
        else -> R.string.notif_message
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .background(if (highlighted) colors.accentSoft else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(n.actor?.avatar_url, who, size = 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(template, who),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.primaryText,
            )
            Text(
                relativeTime(n.created_at),
                style = MaterialTheme.typography.bodySmall,
                color = colors.mutedText,
            )
        }
    }
}

/** Bell for the top bar, with a dot when there are unread notifications. */
@Composable
fun NotificationBell(unread: Int, onClick: () -> Unit) {
    val colors = LocalSwiftsColors.current
    Box(Modifier.size(40.dp).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        AssetIcon(name = "ic_bell", tint = colors.primaryText, contentDescription = stringResource(R.string.notifications_title))
        if (unread > 0) {
            Box(
                Modifier
                    .size(10.dp)
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(colors.accent),
            )
        }
    }
}
