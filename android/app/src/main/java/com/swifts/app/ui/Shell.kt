package com.swifts.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.swifts.app.R
import com.swifts.app.ui.theme.LocalSwiftsColors

// Tabs: 0 home, 1 search, 2 groups (coming soon), 3 profile
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwiftsShell(services: AppServices, onSignOut: () -> Unit) {
    val colors = LocalSwiftsColors.current
    val meId = services.auth.currentUserId().orEmpty()
    var tab by rememberSaveable { mutableStateOf(0) }
    var openProfile by rememberSaveable { mutableStateOf<String?>(null) }
    var showComposer by rememberSaveable { mutableStateOf(false) }

    val feedVm: FeedViewModel = viewModel(
        factory = viewModelFactory {
            initializer { FeedViewModel(services.posts, services.profiles, services.auth, services.lang) }
        },
    )
    val discoverVm: DiscoverViewModel = viewModel(
        factory = viewModelFactory { initializer { DiscoverViewModel(services.profiles, services.auth) } },
    )
    val feedState by feedVm.state.collectAsState()
    val showingProfile = openProfile != null

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (!showingProfile) {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(titleFor(tab)),
                            style = MaterialTheme.typography.headlineSmall,
                            color = colors.primaryText,
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
            }
        },
        bottomBar = {
            SwiftsBottomBar(selected = tab, onSelect = {
                tab = it
                openProfile = null
            })
        },
        floatingActionButton = {
            if (tab == 0 && !showingProfile) {
                FloatingActionButton(
                    onClick = { showComposer = true },
                    containerColor = colors.accent,
                    contentColor = Color.White,
                ) {
                    AssetIcon(
                        name = "ic_plus",
                        tint = Color.White,
                        contentDescription = stringResource(R.string.new_post),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            val pid = openProfile
            when {
                pid != null -> ProfileScreen(
                    services = services,
                    userId = pid,
                    onBack = { openProfile = null },
                    onSignOut = onSignOut,
                )
                tab == 0 -> FeedScreen(feedVm, onOpenProfile = { openProfile = it })
                tab == 1 -> DiscoverScreen(discoverVm, onOpenProfile = { openProfile = it })
                tab == 3 -> ProfileScreen(
                    services = services,
                    userId = meId,
                    onBack = null,
                    onSignOut = onSignOut,
                )
                else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.coming_soon),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.mutedText,
                    )
                }
            }
        }
    }

    if (showComposer) {
        ComposerDialog(
            posting = feedState.posting,
            onDismiss = { showComposer = false },
            onPublish = { text ->
                feedVm.publish(text)
                showComposer = false
            },
        )
    }
}

private fun titleFor(tab: Int): Int = when (tab) {
    0 -> R.string.feed_title
    1 -> R.string.nav_search
    2 -> R.string.nav_groups
    else -> R.string.nav_profile
}

@Composable
private fun SwiftsBottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val colors = LocalSwiftsColors.current
    val items = listOf(
        Triple(R.string.nav_home, "ic_home", 0),
        Triple(R.string.nav_search, "ic_search", 1),
        Triple(R.string.nav_groups, "ic_group", 2),
        Triple(R.string.nav_profile, "ic_person", 3),
    )
    NavigationBar(containerColor = MaterialTheme.colorScheme.background) {
        items.forEach { (labelRes, icon, index) ->
            val isSelected = selected == index
            val tint = if (isSelected) colors.accent else colors.mutedText
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelect(index) },
                icon = { AssetIcon(name = icon, tint = tint, contentDescription = null) },
                label = {
                    Text(
                        stringResource(labelRes),
                        style = MaterialTheme.typography.labelSmall,
                        color = tint,
                    )
                },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(indicatorColor = colors.accentSoft),
            )
        }
    }
}
