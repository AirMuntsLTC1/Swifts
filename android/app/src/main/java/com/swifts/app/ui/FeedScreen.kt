package com.swifts.app.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.swifts.app.R
import com.swifts.app.domain.Post
import com.swifts.app.ui.theme.LocalSwiftsColors

@Composable
fun FeedScreen(vm: FeedViewModel, onOpenProfile: (String) -> Unit) {
    val state by vm.state.collectAsState()
    Column(Modifier.fillMaxSize()) {
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
        FeedBody(state = state, onLike = vm::toggleLike, onOpenProfile = onOpenProfile)
    }
}

@Composable
private fun FeedBody(
    state: FeedUiState,
    onLike: (String) -> Unit,
    onOpenProfile: (String) -> Unit,
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
            item { SectionLabel(stringResource(R.string.section_recent)) }
            state.error?.let { err ->
                item {
                    Text(
                        err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }
            items(state.items, key = { it.id }) { post ->
                PostItem(
                    post = post,
                    liked = post.id in state.liked,
                    onLike = { onLike(post.id) },
                    onOpenProfile = { onOpenProfile(post.author_id) },
                )
            }
        }
    }
}

@Composable
private fun PostItem(post: Post, liked: Boolean, onLike: () -> Unit, onOpenProfile: () -> Unit) {
    val colors = LocalSwiftsColors.current
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
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ActionLabel(stringResource(if (liked) R.string.liked else R.string.like), active = liked, onClick = onLike)
            Spacer(Modifier.width(20.dp))
            ActionLabel(stringResource(R.string.comment), active = false, onClick = {})
            Spacer(Modifier.width(20.dp))
            ActionLabel(stringResource(R.string.share), active = false, onClick = {})
        }
    }
}

@Composable
private fun ActionLabel(text: String, active: Boolean, onClick: () -> Unit) {
    val colors = LocalSwiftsColors.current
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = if (active) colors.accent else colors.mutedText,
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
