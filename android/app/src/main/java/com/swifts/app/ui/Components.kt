package com.swifts.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.swifts.app.R
import com.swifts.app.ui.theme.LocalSwiftsColors

@Composable
fun Avatar(url: String?, name: String, size: Dp = 44.dp) {
    val colors = LocalSwiftsColors.current
    Box(
        Modifier.size(size).clip(CircleShape).background(colors.neutralPill),
        contentAlignment = Alignment.Center,
    ) {
        if (url.isNullOrBlank()) {
            Text(
                name.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleMedium,
                color = colors.primaryText,
            )
        } else {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    val colors = LocalSwiftsColors.current
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = colors.primaryText,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
    )
}

/** Rounded pill button: neutral when [active], brand accent when not. */
@Composable
fun PillButton(active: Boolean, activeText: String, idleText: String, onClick: () -> Unit) {
    val colors = LocalSwiftsColors.current
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (active) colors.neutralPill else colors.accent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            if (active) activeText else idleText,
            style = MaterialTheme.typography.labelLarge,
            color = if (active) colors.primaryText else Color.White,
        )
    }
}

@Composable
fun FollowButton(following: Boolean, onClick: () -> Unit) {
    PillButton(
        active = following,
        activeText = stringResource(R.string.following),
        idleText = stringResource(R.string.follow),
        onClick = onClick,
    )
}
