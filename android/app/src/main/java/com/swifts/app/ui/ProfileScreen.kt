package com.swifts.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.swifts.app.domain.Post
import com.swifts.app.domain.Profile
import com.swifts.app.data.ThemeMode
import com.swifts.app.domain.ProfileUpdate
import com.swifts.app.ui.theme.LocalSwiftsColors

@Composable
fun ProfileScreen(
    services: AppServices,
    userId: String,
    onBack: (() -> Unit)?,
    onSignOut: () -> Unit,
    onOpenChat: (String, String) -> Unit,
) {
    val vm: ProfileViewModel = viewModel(
        key = "profile-$userId",
        factory = viewModelFactory {
            initializer { ProfileViewModel(services.profiles, services.posts, services.auth, userId) }
        },
    )
    val state by vm.state.collectAsState()
    val colors = LocalSwiftsColors.current
    val profile = state.profile
    val scope = rememberCoroutineScope()
    val themeMode by services.sessionStore.themeMode.collectAsState()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        if (onBack != null) {
            Text(
                "← " + stringResource(R.string.back),
                style = MaterialTheme.typography.labelLarge,
                color = colors.accent,
                modifier = Modifier.clickable(onClick = onBack).padding(vertical = 8.dp),
            )
        }

        when {
            state.loading && profile == null -> Box(
                Modifier.fillMaxWidth().padding(top = 48.dp),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            profile == null -> Text(
                state.error ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge,
            )

            else -> ProfileBody(
                profile = profile,
                state = state,
                onToggleFollow = vm::toggleFollow,
                onEdit = { vm.setEditing(true) },
                onSignOut = onSignOut,
                onMessage = {
                    scope.launch {
                        services.messages.startConversation(userId).onSuccess { id ->
                            onOpenChat(id, profile?.username.orEmpty())
                        }
                    }
                },
                themeMode = themeMode,
                onThemeChange = services.sessionStore::setThemeMode,
            )
        }
    }

    if (state.editing && profile != null) {
        EditProfileDialog(
            profile = profile,
            saving = state.saving,
            onDismiss = { vm.setEditing(false) },
            onSave = vm::save,
        )
    }
}

@Composable
private fun ProfileBody(
    profile: Profile,
    state: ProfileUiState,
    onToggleFollow: () -> Unit,
    onEdit: () -> Unit,
    onSignOut: () -> Unit,
    onMessage: () -> Unit,
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
) {
    val colors = LocalSwiftsColors.current
    val name = profile.display_name.ifBlank { profile.username }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Avatar(profile.avatar_url, name, size = 84.dp)
        Spacer(Modifier.width(16.dp))
        Column {
            Text(name, style = MaterialTheme.typography.headlineSmall, color = colors.primaryText)
            Text("@${profile.username}", style = MaterialTheme.typography.titleSmall, color = colors.mutedText)
        }
    }

    if (profile.bio.isNotBlank()) {
        Text(
            profile.bio,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.primaryText,
            modifier = Modifier.padding(top = 12.dp),
        )
    }

    DetailRow(stringResource(R.string.label_city), profile.city)
    DetailRow(stringResource(R.string.label_hometown), profile.hometown)
    DetailRow(stringResource(R.string.label_work), profile.work)
    DetailRow(stringResource(R.string.label_school), profile.school)

    Spacer(Modifier.height(16.dp))
    if (state.isMe) {
        SectionLabel(stringResource(R.string.section_appearance))
        ThemeSelector(current = themeMode, onSelect = onThemeChange)
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.edit_profile), style = MaterialTheme.typography.labelLarge)
        }
        TextButton(onClick = onSignOut) {
            Text(stringResource(R.string.sign_out), style = MaterialTheme.typography.labelLarge, color = colors.accent)
        }
    } else {
        FollowButton(following = state.following, onClick = onToggleFollow)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onMessage, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.message), style = MaterialTheme.typography.labelLarge)
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
    state.posts.forEach { PostPreview(it) }
}

/** Light, dark, or follow the system. The choice is kept on this device. */
@Composable
private fun ThemeSelector(current: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val colors = LocalSwiftsColors.current
    Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            ThemeMode.SYSTEM to R.string.theme_system,
            ThemeMode.LIGHT to R.string.theme_light,
            ThemeMode.DARK to R.string.theme_dark,
        ).forEach { (mode, label) ->
            val selected = mode == current
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) colors.accentSoft else colors.neutralPill)
                    .clickable(role = Role.RadioButton) { onSelect(mode) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
                Text(
                    stringResource(label),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) colors.accent else colors.primaryText,
                )
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    val colors = LocalSwiftsColors.current
    Row(Modifier.padding(top = 6.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = colors.mutedText, modifier = Modifier.width(80.dp))
        Text(value, style = MaterialTheme.typography.bodyLarge, color = colors.primaryText)
    }
}

@Composable
private fun PostPreview(post: Post) {
    val colors = LocalSwiftsColors.current
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text(relativeTime(post.created_at), style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
        Text(post.body, style = MaterialTheme.typography.bodyLarge, color = colors.primaryText)
    }
}

@Composable
private fun EditProfileDialog(
    profile: Profile,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (ProfileUpdate) -> Unit,
) {
    var displayName by rememberSaveable { mutableStateOf(profile.display_name) }
    var bio by rememberSaveable { mutableStateOf(profile.bio) }
    var city by rememberSaveable { mutableStateOf(profile.city.orEmpty()) }
    var hometown by rememberSaveable { mutableStateOf(profile.hometown.orEmpty()) }
    var work by rememberSaveable { mutableStateOf(profile.work.orEmpty()) }
    var school by rememberSaveable { mutableStateOf(profile.school.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_profile), style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                EditField(R.string.field_display_name, displayName) { displayName = it }
                EditField(R.string.field_bio, bio) { bio = it }
                EditField(R.string.label_city, city) { city = it }
                EditField(R.string.label_hometown, hometown) { hometown = it }
                EditField(R.string.label_work, work) { work = it }
                EditField(R.string.label_school, school) { school = it }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving,
                onClick = {
                    onSave(
                        ProfileUpdate(
                            display_name = displayName.trim(),
                            bio = bio.trim(),
                            city = city.trim().takeIf { it.isNotEmpty() },
                            hometown = hometown.trim().takeIf { it.isNotEmpty() },
                            work = work.trim().takeIf { it.isNotEmpty() },
                            school = school.trim().takeIf { it.isNotEmpty() },
                        ),
                    )
                },
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun EditField(labelRes: Int, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(labelRes)) },
        singleLine = labelRes != R.string.field_bio,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    )
}

