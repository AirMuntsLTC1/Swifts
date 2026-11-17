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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.swifts.app.R
import com.swifts.app.domain.Group
import com.swifts.app.ui.theme.LocalSwiftsColors

@Composable
fun GroupsScreen(vm: GroupsViewModel, onOpen: (String) -> Unit) {
    val state by vm.state.collectAsState()
    val colors = LocalSwiftsColors.current

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = vm::onQueryChange,
            placeholder = {
                Text(stringResource(R.string.search_groups_hint), style = MaterialTheme.typography.bodyLarge, color = colors.mutedText)
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        OutlinedButton(
            onClick = { vm.setShowCreate(true) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Text(stringResource(R.string.create_group), style = MaterialTheme.typography.labelLarge)
        }
        state.error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        SectionLabel(stringResource(R.string.section_groups))

        LazyColumn(Modifier.weight(1f)) {
            items(state.groups, key = { it.id }) { g ->
                GroupRow(
                    group = g,
                    joined = g.id in state.joined,
                    onOpen = { onOpen(g.id) },
                    onToggle = { vm.toggleJoin(g) },
                )
            }
        }
    }

    if (state.showCreate) {
        CreateGroupDialog(
            creating = state.creating,
            onDismiss = { vm.setShowCreate(false) },
            onCreate = vm::create,
        )
    }
}

@Composable
private fun GroupRow(group: Group, joined: Boolean, onOpen: () -> Unit, onToggle: () -> Unit) {
    val colors = LocalSwiftsColors.current
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(group.name, style = MaterialTheme.typography.titleSmall, color = colors.primaryText)
            if (group.description.isNotBlank()) {
                Text(
                    group.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mutedText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                stringResource(privacyLabel(group.privacy)),
                style = MaterialTheme.typography.bodySmall,
                color = colors.mutedText,
            )
        }
        Spacer(Modifier.width(12.dp))
        PillButton(
            active = joined,
            activeText = stringResource(R.string.joined),
            idleText = stringResource(R.string.join),
            onClick = onToggle,
        )
    }
}

@Composable
private fun CreateGroupDialog(
    creating: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String, String, String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var privacy by rememberSaveable { mutableStateOf("public") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.create_group), style = MaterialTheme.typography.titleMedium) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.group_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.padding(top = 8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.group_description)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    PillButton(
                        active = privacy == "public",
                        activeText = stringResource(R.string.privacy_public),
                        idleText = stringResource(R.string.privacy_public),
                        onClick = { privacy = "public" },
                    )
                    PillButton(
                        active = privacy == "private",
                        activeText = stringResource(R.string.privacy_private),
                        idleText = stringResource(R.string.privacy_private),
                        onClick = { privacy = "private" },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !creating && name.isNotBlank(),
                onClick = { onCreate(name, description, privacy) },
            ) { Text(stringResource(R.string.create)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

private fun privacyLabel(privacy: String): Int =
    if (privacy == "private") R.string.privacy_private else R.string.privacy_public
