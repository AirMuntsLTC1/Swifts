package com.swifts.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.swifts.app.R
import com.swifts.app.domain.Post
import com.swifts.app.ui.theme.LocalSwiftsColors

@Composable
fun GroupScreen(services: AppServices, groupId: String, onBack: () -> Unit) {
    val vm: GroupDetailViewModel = viewModel(
        key = "group-$groupId",
        factory = viewModelFactory {
            initializer {
                GroupDetailViewModel(services.groups, services.posts, services.auth, groupId, services.lang)
            }
        },
    )
    val state by vm.state.collectAsState()
    val colors = LocalSwiftsColors.current
    var draft by rememberSaveable { mutableStateOf("") }
    val group = state.group

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            "← " + stringResource(R.string.back),
            style = MaterialTheme.typography.labelLarge,
            color = colors.accent,
            modifier = Modifier.clickable(onClick = onBack).padding(vertical = 8.dp),
        )

        when {
            group == null && state.loading -> Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            group == null -> Text(
                state.error ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge,
            )

            else -> {
                Text(group.name, style = MaterialTheme.typography.headlineSmall, color = colors.primaryText)
                if (group.description.isNotBlank()) {
                    Text(
                        group.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.primaryText,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(if (group.privacy == "private") R.string.privacy_private else R.string.privacy_public),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.mutedText,
                    )
                    Spacer(Modifier.width(12.dp))
                    PillButton(
                        active = state.joined,
                        activeText = stringResource(R.string.joined),
                        idleText = stringResource(R.string.join),
                        onClick = vm::toggleMembership,
                    )
                }

                if (state.joined) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        placeholder = {
                            Text(stringResource(R.string.group_post_hint), style = MaterialTheme.typography.bodyLarge, color = colors.mutedText)
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    )
                    Button(
                        onClick = {
                            vm.publish(draft)
                            draft = ""
                        },
                        enabled = draft.isNotBlank() && !state.posting,
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Text(stringResource(R.string.publish), style = MaterialTheme.typography.labelLarge)
                    }
                }

                SectionLabel(stringResource(R.string.section_posts))
                if (state.posts.isEmpty()) {
                    Text(
                        stringResource(R.string.no_posts_yet),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.mutedText,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
                LazyColumn(Modifier.weight(1f)) {
                    items(state.posts, key = { it.id }) { post -> GroupPostRow(post) }
                }
            }
        }
    }
}

@Composable
private fun GroupPostRow(post: Post) {
    val colors = LocalSwiftsColors.current
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text(
            post.author?.let { "@${it.username}" } ?: stringResource(R.string.you),
            style = MaterialTheme.typography.titleSmall,
            color = colors.primaryText,
        )
        Text(relativeTime(post.created_at), style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
        Text(post.body, style = MaterialTheme.typography.bodyLarge, color = colors.primaryText)
    }
}
