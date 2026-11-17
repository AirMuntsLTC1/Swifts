package com.swifts.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.swifts.app.R
import com.swifts.app.domain.Profile
import com.swifts.app.ui.theme.LocalSwiftsColors

@Composable
fun DiscoverScreen(vm: DiscoverViewModel, onOpenProfile: (String) -> Unit) {
    val state by vm.state.collectAsState()
    val colors = LocalSwiftsColors.current

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = vm::onQueryChange,
            placeholder = {
                Text(stringResource(R.string.search_hint), style = MaterialTheme.typography.bodyLarge, color = colors.mutedText)
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        state.error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        if (state.query.isNotBlank()) SectionLabel(stringResource(R.string.section_people))

        LazyColumn {
            items(state.results, key = { it.id }) { p ->
                ProfileRow(
                    profile = p,
                    following = p.id in state.following,
                    onOpen = { onOpenProfile(p.id) },
                    onFollow = { vm.toggleFollow(p) },
                )
            }
        }
    }
}

@Composable
private fun ProfileRow(profile: Profile, following: Boolean, onOpen: () -> Unit, onFollow: () -> Unit) {
    val colors = LocalSwiftsColors.current
    val name = profile.display_name.ifBlank { profile.username }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(profile.avatar_url, name, size = 44.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleSmall, color = colors.primaryText)
            Text("@${profile.username}", style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
        }
        FollowButton(following = following, onClick = onFollow)
    }
}
