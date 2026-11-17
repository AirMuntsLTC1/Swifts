package com.swifts.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.swifts.app.R
import com.swifts.app.domain.Post
import com.swifts.app.ui.theme.LocalSwiftsColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(vm: FeedViewModel, onSignOut: () -> Unit) {
    val state by vm.state.collectAsState()
    val colors = LocalSwiftsColors.current
    var showComposer by rememberSaveable { mutableStateOf(false) }
    var tab by rememberSaveable { mutableStateOf(0) }      // 0 home, 1 search, 2 groups, 3 profile
    var filter by rememberSaveable { mutableStateOf(0) }   // 0 all, 1 following

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.feed_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.primaryText,
                    )
                },
                actions = {
                    TextButton(onClick = onSignOut) {
                        Text(
                            stringResource(R.string.sign_out),
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.accent,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = { SwiftsBottomBar(selected = tab, onSelect = { tab = it }) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showComposer = true },
                containerColor = colors.accent,
                contentColor = androidx.compose.ui.graphics.Color.White,
            ) {
                Icon(painterResource(R.drawable.ic_plus), contentDescription = stringResource(R.string.new_post))
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (tab != 0) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.coming_soon),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.mutedText,
                    )
                }
            } else {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterPill(stringResource(R.string.pill_all), selected = filter == 0) { filter = 0 }
                    FilterPill(stringResource(R.string.pill_following), selected = filter == 1) { filter = 1 }
                }
                FeedBody(state = state, showingFollowing = filter == 1, onLike = vm::toggleLike)
            }
        }
    }

    if (showComposer) {
        ComposerDialog(
            posting = state.posting,
            onDismiss = { showComposer = false },
            onPublish = { text ->
                vm.publish(text)
                showComposer = false
            },
        )
    }
}

@Composable
private fun FeedBody(state: FeedUiState, showingFollowing: Boolean, onLike: (String) -> Unit) {
    val colors = LocalSwiftsColors.current
    when {
        showingFollowing -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.following_soon),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.mutedText,
                textAlign = TextAlign.Center,
            )
        }

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
                PostItem(post = post, liked = post.id in state.liked, onLike = { onLike(post.id) })
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    val colors = LocalSwiftsColors.current
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = colors.primaryText,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun PostItem(post: Post, liked: Boolean, onLike: () -> Unit) {
    val colors = LocalSwiftsColors.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(
            post.author?.let { "@${it.username}" } ?: stringResource(R.string.you),
            style = MaterialTheme.typography.titleSmall,
            color = colors.primaryText,
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

@Composable
private fun SwiftsBottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val colors = LocalSwiftsColors.current
    val items = listOf(
        Triple(R.string.nav_home, R.drawable.ic_home, 0),
        Triple(R.string.nav_search, R.drawable.ic_search, 1),
        Triple(R.string.nav_groups, R.drawable.ic_group, 2),
        Triple(R.string.nav_profile, R.drawable.ic_person, 3),
    )
    NavigationBar(containerColor = MaterialTheme.colorScheme.background) {
        items.forEach { (labelRes, iconRes, index) ->
            val isSelected = selected == index
            val tint = if (isSelected) colors.accent else colors.mutedText
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelect(index) },
                icon = { Icon(painterResource(iconRes), contentDescription = null, tint = tint) },
                label = {
                    Text(
                        stringResource(labelRes),
                        style = MaterialTheme.typography.labelSmall,
                        color = tint,
                    )
                },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = colors.accentSoft,
                ),
            )
        }
    }
}
