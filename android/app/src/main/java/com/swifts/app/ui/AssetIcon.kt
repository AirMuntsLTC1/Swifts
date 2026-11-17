package com.swifts.app.ui

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/** Draws assets/icons/<name>.svg, tinted to [tint]. */
@Composable
fun AssetIcon(
    name: String,
    tint: Color,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
) {
    AsyncImage(
        model = "file:///android_asset/icons/$name.svg",
        contentDescription = contentDescription,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier.size(size),
    )
}
