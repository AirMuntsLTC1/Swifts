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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.swifts.app.R
import com.swifts.app.domain.ConversationSummary
import com.swifts.app.ui.theme.LocalSwiftsColors

@Composable
fun MessagesScreen(vm: MessagesViewModel, onOpen: (String, String) -> Unit) {
    val state by vm.state.collectAsState()
    val colors = LocalSwiftsColors.current

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        }
        SectionLabel(stringResource(R.string.section_chats))

        if (state.items.isEmpty() && !state.loading) {
            Text(
                stringResource(R.string.no_chats),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.mutedText,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
            )
        }

        LazyColumn(Modifier.weight(1f)) {
            items(state.items, key = { it.id }) { c -> ConversationRow(c, onOpen = { onOpen(c.id, c.name) }) }
        }
    }
}

@Composable
private fun ConversationRow(c: ConversationSummary, onOpen: () -> Unit) {
    val colors = LocalSwiftsColors.current
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(c.avatarUrl, c.name, size = 48.dp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(c.name, style = MaterialTheme.typography.titleSmall, color = colors.primaryText)
            Text("@${c.username}", style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
        }
    }
}
