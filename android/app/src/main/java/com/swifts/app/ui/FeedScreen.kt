package com.swifts.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.swifts.app.R
import com.swifts.app.domain.Post
import com.swifts.app.domain.PostMedia
import com.swifts.app.ui.theme.LocalSwiftsColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    vm: FeedViewModel,
    onOpenProfile: (String) -> Unit,
    onComment: (String, String) -> Unit = { _, _ -> },
    mediaUrl: (String) -> String = { it },
    header: @Composable () -> Unit = {},
) {
    val state by vm.state.collectAsState()
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var deletingId by rememberSaveable { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        header()
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterPill(stringResource(R.string.pill_all), selected = !state.showFollowing) {
                vm.setShowFollowing(false)
            }
            FilterPill(stringResource(R.string.pill_following), selected = state.showFollowing) {
                vm.setShowFollowing(true)
            }
        }
        Box(Modifier.weight(1f)) {
            PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = vm::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                FeedBody(
                    state = state,
                    meId = vm.meId,
                    mediaUrl = mediaUrl,
                    onLike = vm::toggleLike,
                    onOpenProfile = onOpenProfile,
                    onEdit = { editingId = it },
                    onDelete = { deletingId = it },
                    onComment = onComment,
                )
            }
        }
    }

    state.items.find { it.id == editingId }?.let { post ->
        EditPostDialog(
            initial = post.body,
            onDismiss = { editingId = null },
            onSave = { text ->
                vm.updatePost(post.id, text)
                editingId = null
            },
        )
    }

    deletingId?.let { id ->
        AlertDialog(
            onDismissRequest = { deletingId = null },
            title = { Text(stringResource(R.string.delete_post_title)) },
            text = { Text(stringResource(R.string.delete_post_body)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.deletePost(id)
                    deletingId = null
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingId = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun FeedBody(
    state: FeedUiState,
    meId: String,
    mediaUrl: (String) -> String,
    onLike: (String) -> Unit,
    onOpenProfile: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    onComment: (String, String) -> Unit,
) {
    val colors = LocalSwiftsColors.current
    when {
        state.loading && state.items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        state.items.isEmpty() -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                state.error ?: stringResource(R.string.feed_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.mutedText,
                textAlign = TextAlign.Center,
            )
        }

        else -> LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
            item(key = "section-recent", contentType = "label") {
                SectionLabel(stringResource(R.string.section_recent))
            }
            state.error?.let { err ->
                item(key = "feed-error", contentType = "error") {
                    Text(
                        err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }
            // Stable keys and a shared content type let Compose reuse and move rows instead of rebuilding them.
            items(state.items, key = { it.id }, contentType = { "post" }) { post ->
                PostItem(
                    post = post,
                    liked = post.id in state.liked,
                    isMine = post.author_id == meId,
                    mediaUrl = mediaUrl,
                    onLike = { onLike(post.id) },
                    onOpenProfile = { onOpenProfile(post.author_id) },
                    onEdit = { onEdit(post.id) },
                    onDelete = { onDelete(post.id) },
                    onComment = { onComment(post.id, post.author_id) },
                )
            }
        }
    }
}

@Composable
private fun PostItem(
    post: Post,
    liked: Boolean,
    isMine: Boolean,
    mediaUrl: (String) -> String,
    onLike: () -> Unit,
    onOpenProfile: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onComment: () -> Unit,
) {
    val colors = LocalSwiftsColors.current
    Column {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(
                post.author?.let { "@${it.username}" } ?: stringResource(R.string.you),
                style = MaterialTheme.typography.titleSmall,
                color = colors.primaryText,
                modifier = Modifier.clickable(onClick = onOpenProfile),
            )
            Text(
                "${relativeTime(post.created_at)} · ${visibilityLabel(post.visibility)}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.mutedText,
            )
            Spacer(Modifier.height(6.dp))
            Text(post.body, style = MaterialTheme.typography.bodyLarge, color = colors.primaryText)
            if (post.post_media.any { it.kind == "image" }) {
                Spacer(Modifier.height(8.dp))
                PostImages(post.post_media.filter { it.kind == "image" }, mediaUrl)
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ActionLabel(stringResource(if (liked) R.string.liked else R.string.like), active = liked, onClick = onLike)
                Spacer(Modifier.width(20.dp))
                ActionLabel(stringResource(R.string.comment), active = false, onClick = onComment)
                Spacer(Modifier.width(20.dp))
                ActionLabel(stringResource(R.string.share), active = false, onClick = {})
                if (isMine) {
                    Spacer(Modifier.width(20.dp))
                    ActionLabel(stringResource(R.string.edit_post), active = false, onClick = onEdit)
                    Spacer(Modifier.width(20.dp))
                    ActionLabel(stringResource(R.string.delete), active = false, onClick = onDelete, danger = true)
                }
            }
        }
        // Hairline between posts.
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/** One image full width, or up to four in a two-column grid. Each tile has a fixed aspect ratio so rows never jump when images load. */
@Composable
private fun PostImages(media: List<PostMedia>, mediaUrl: (String) -> String) {
    val shown = media.take(4)
    if (shown.size == 1) {
        PostImage(mediaUrl(shown[0].storage_path), Modifier.fillMaxWidth().aspectRatio(4f / 3f))
    } else {
        shown.chunked(2).forEachIndexed { rowIndex, pair ->
            if (rowIndex > 0) Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                pair.forEach { m ->
                    PostImage(mediaUrl(m.storage_path), Modifier.weight(1f).aspectRatio(1f))
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PostImage(url: String, modifier: Modifier) {
    val context = LocalContext.current
    AsyncImage(
        model = ImageRequest.Builder(context).data(url).crossfade(true).build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(LocalSwiftsColors.current.neutralPill),
    )
}

@Composable
private fun EditPostDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_post)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { if (it.length <= MAX_POST_LENGTH) text = it },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(enabled = text.isNotBlank(), onClick = { onSave(text) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun ActionLabel(text: String, active: Boolean, onClick: () -> Unit, danger: Boolean = false) {
    val colors = LocalSwiftsColors.current
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = when {
            active -> colors.accent
            danger -> MaterialTheme.colorScheme.error
            else -> colors.mutedText
        },
        modifier = Modifier.clickable(onClick = onClick).padding(vertical = 6.dp),
    )
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalSwiftsColors.current
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) colors.accentSoft else colors.neutralPill)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) colors.accent else colors.primaryText,
        )
    }
}

@Composable
private fun visibilityLabel(visibility: String): String = when (visibility) {
    "followers" -> stringResource(R.string.visibility_followers)
    "private" -> stringResource(R.string.visibility_private)
    else -> stringResource(R.string.visibility_public)
}
