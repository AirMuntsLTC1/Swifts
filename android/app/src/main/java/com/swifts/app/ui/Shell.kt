package com.swifts.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.swifts.app.R
import com.swifts.app.domain.POST_MEDIA_BUCKET
import com.swifts.app.ui.theme.LocalSwiftsColors

/** Every full-screen destination. Tabs use index 0 home, 1 search, 2 groups, 3 messages, 4 profile. */
private sealed interface Screen {
    data object Notifications : Screen
    data class Profile(val userId: String) : Screen
    data class Group(val groupId: String) : Screen
    data class Chat(val conversationId: String, val title: String) : Screen
    data class Tab(val index: Int) : Screen
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwiftsShell(services: AppServices, onSignOut: () -> Unit) {
    val colors = LocalSwiftsColors.current
    val meId = services.auth.currentUserId().orEmpty()
    var tab by rememberSaveable { mutableStateOf(0) }
    var openProfile by rememberSaveable { mutableStateOf<String?>(null) }
    var openGroup by rememberSaveable { mutableStateOf<String?>(null) }
    var openChatId by rememberSaveable { mutableStateOf<String?>(null) }
    var openChatTitle by rememberSaveable { mutableStateOf("") }
    var openNotifications by rememberSaveable { mutableStateOf(false) }
    var showComposer by rememberSaveable { mutableStateOf(false) }
    var commentingPost by rememberSaveable { mutableStateOf<String?>(null) }
    var commentingAuthor by rememberSaveable { mutableStateOf("") }

    val feedVm: FeedViewModel = viewModel(
        factory = viewModelFactory {
            initializer { FeedViewModel(services.posts, services.profiles, services.auth, services.storage, services.lang) }
        },
    )
    val discoverVm: DiscoverViewModel = viewModel(
        factory = viewModelFactory { initializer { DiscoverViewModel(services.profiles, services.auth) } },
    )
    val groupsVm: GroupsViewModel = viewModel(
        factory = viewModelFactory { initializer { GroupsViewModel(services.groups, services.auth) } },
    )
    val messagesVm: MessagesViewModel = viewModel(
        factory = viewModelFactory { initializer { MessagesViewModel(services.messages, services.auth) } },
    )
    val notifVm: NotificationsViewModel = viewModel(
        factory = viewModelFactory { initializer { NotificationsViewModel(services.notifications, services.auth) } },
    )
    val notifState by notifVm.state.collectAsState()
    val feedState by feedVm.state.collectAsState()

    val showingSubpage = openProfile != null || openGroup != null || openChatId != null || openNotifications
    fun closeSubpages() {
        openProfile = null
        openGroup = null
        openChatId = null
        openNotifications = false
    }

    // Derive the current destination from the saved state. AnimatedContent keeps each screen's own value while it fades out.
    val pid = openProfile
    val gid = openGroup
    val cid = openChatId
    val screen: Screen = when {
        openNotifications -> Screen.Notifications
        pid != null -> Screen.Profile(pid)
        gid != null -> Screen.Group(gid)
        cid != null -> Screen.Chat(cid, openChatTitle)
        else -> Screen.Tab(tab)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (!showingSubpage) {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(titleFor(tab)),
                            style = MaterialTheme.typography.headlineSmall,
                            color = colors.primaryText,
                        )
                    },
                    actions = {
                        NotificationBell(unread = notifState.unread, onClick = { openNotifications = true })
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
            }
        },
        bottomBar = {
            SwiftsBottomBar(selected = tab, onSelect = {
                tab = it
                closeSubpages()
            })
        },
        floatingActionButton = {
            if (tab == 0 && !showingSubpage) {
                FloatingActionButton(
                    onClick = { showComposer = true },
                    containerColor = colors.accent,
                    contentColor = Color.White,
                ) {
                    AssetIcon(name = "ic_plus", tint = Color.White, contentDescription = stringResource(R.string.new_post))
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            AnimatedContent(
                targetState = screen,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(180)) +
                        slideInHorizontally(animationSpec = tween(180)) { it / 12 })
                        .togetherWith(fadeOut(animationSpec = tween(120)))
                },
                label = "screen",
            ) { s ->
                when (s) {
                    Screen.Notifications -> NotificationsScreen(
                        vm = notifVm,
                        onBack = { openNotifications = false },
                        onOpenProfile = { id ->
                            openNotifications = false
                            openProfile = id
                        },
                    )
                    is Screen.Profile -> ProfileScreen(
                        services = services,
                        userId = s.userId,
                        onBack = { openProfile = null },
                        onSignOut = onSignOut,
                        onOpenChat = { id, title ->
                            openProfile = null
                            openChatId = id
                            openChatTitle = title
                        },
                    )
                    is Screen.Group -> GroupScreen(
                        services = services,
                        groupId = s.groupId,
                        onBack = { openGroup = null },
                    )
                    is Screen.Chat -> ChatScreen(
                        services = services,
                        conversationId = s.conversationId,
                        title = s.title,
                        onBack = { openChatId = null },
                    )
                    is Screen.Tab -> when (s.index) {
                        0 -> FeedScreen(
                            vm = feedVm,
                            onOpenProfile = { openProfile = it },
                            mediaUrl = { services.storage.publicUrl(POST_MEDIA_BUCKET, it) },
                            header = { StoriesRow(services) },
                            onComment = { postId, authorId ->
                                commentingPost = postId
                                commentingAuthor = authorId
                            },
                        )
                        1 -> DiscoverScreen(discoverVm, onOpenProfile = { openProfile = it })
                        2 -> GroupsScreen(groupsVm, onOpen = { openGroup = it })
                        3 -> MessagesScreen(messagesVm, onOpen = { id, title ->
                            openChatId = id
                            openChatTitle = title
                        })
                        else -> ProfileScreen(
                            services = services,
                            userId = meId,
                            onBack = null,
                            onSignOut = onSignOut,
                            onOpenChat = { id, title ->
                                openChatId = id
                                openChatTitle = title
                            },
                        )
                    }
                }
            }
        }
    }

    commentingPost?.let { postId ->
        CommentsDialog(
            services = services,
            postId = postId,
            postAuthorId = commentingAuthor,
            onClose = { commentingPost = null },
        )
    }

    if (showComposer) {
        ComposerDialog(
            posting = feedState.posting,
            onDismiss = { showComposer = false },
            onPublish = { text, media ->
                feedVm.publish(text, media)
                showComposer = false
            },
        )
    }
}

private fun titleFor(tab: Int): Int = when (tab) {
    0 -> R.string.feed_title
    1 -> R.string.nav_search
    2 -> R.string.nav_groups
    3 -> R.string.nav_messages
    else -> R.string.nav_profile
}

@Composable
private fun SwiftsBottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val colors = LocalSwiftsColors.current
    val items = listOf(
        Triple(R.string.nav_home, "ic_home", 0),
        Triple(R.string.nav_search, "ic_search", 1),
        Triple(R.string.nav_groups, "ic_group", 2),
        Triple(R.string.nav_messages, "ic_chat", 3),
        Triple(R.string.nav_profile, "ic_person", 4),
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
                    Text(stringResource(labelRes), style = MaterialTheme.typography.labelSmall, color = tint)
                },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(indicatorColor = colors.accentSoft),
            )
        }
    }
}

