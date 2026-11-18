package com.swifts.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import com.swifts.app.R
import com.swifts.app.ui.theme.LocalSwiftsColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Horizontal story bubbles with an "add" bubble first. Sits above the Home feed. */
@Composable
fun StoriesRow(services: AppServices) {
    val vm: StoriesViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                StoriesViewModel(services.stories, services.profiles, services.storage, services.auth)
            }
        },
    )
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }
                if (bytes != null) vm.upload(bytes, mime)
            }
        }
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { AddStoryBubble(uploading = state.uploading, onClick = { picker.launch("image/*") }) }
        items(state.bubbles, key = { it.authorId }) { bubble ->
            StoryAvatar(bubble, onClick = { vm.open(bubble) })
        }
    }

    state.error?.let {
        Text(
            it,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }

    state.viewing?.let { bubble ->
        StoryViewer(bubble, urlOf = vm::publicUrl, onClose = vm::close)
    }
}

@Composable
private fun AddStoryBubble(uploading: Boolean, onClick: () -> Unit) {
    val colors = LocalSwiftsColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Box(
            Modifier.size(60.dp).clip(CircleShape).background(colors.neutralPill),
            contentAlignment = Alignment.Center,
        ) {
            AssetIcon(name = "ic_plus", tint = colors.accent, contentDescription = stringResource(R.string.your_story))
        }
        Text(
            if (uploading) stringResource(R.string.uploading) else stringResource(R.string.your_story),
            style = MaterialTheme.typography.bodySmall,
            color = colors.mutedText,
        )
    }
}

@Composable
private fun StoryAvatar(bubble: StoryBubble, onClick: () -> Unit) {
    val colors = LocalSwiftsColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Box(
            Modifier.size(64.dp).clip(CircleShape).border(2.dp, colors.accent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Avatar(bubble.avatarUrl, bubble.username, size = 56.dp)
        }
        Text(
            "@${bubble.username}",
            style = MaterialTheme.typography.bodySmall,
            color = colors.mutedText,
        )
    }
}

@Composable
private fun StoryViewer(bubble: StoryBubble, urlOf: (String) -> String, onClose: () -> Unit) {
    var index by remember(bubble.authorId) { mutableStateOf(0) }
    val colors = LocalSwiftsColors.current

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .clickable {
                    if (index < bubble.stories.size - 1) index++ else onClose()
                },
        ) {
            AsyncImage(
                model = urlOf(bubble.stories[index].media_path),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            Row(
                Modifier.align(Alignment.TopStart).fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Avatar(bubble.avatarUrl, bubble.username, size = 36.dp)
                Spacer(Modifier.width(8.dp))
                Text("@${bubble.username}", style = MaterialTheme.typography.titleSmall, color = colors.primaryText)
                Spacer(Modifier.weight(1f))
                Text(
                    stringResource(R.string.close),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.accent,
                    modifier = Modifier.clickable(onClick = onClose),
                )
            }
        }
    }
}
