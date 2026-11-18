package com.swifts.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.swifts.app.R
import com.swifts.app.domain.Message
import com.swifts.app.ui.theme.LocalSwiftsColors

@Composable
fun ChatScreen(services: AppServices, conversationId: String, title: String, onBack: () -> Unit) {
    val vm: ChatViewModel = viewModel(
        key = "chat-$conversationId",
        factory = viewModelFactory {
            initializer { ChatViewModel(services.messages, services.auth, conversationId) }
        },
    )
    val state by vm.state.collectAsState()
    val colors = LocalSwiftsColors.current
    val me = services.auth.currentUserId()
    val listState = rememberLazyListState()
    var draft by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            "← " + stringResource(R.string.back),
            style = MaterialTheme.typography.labelLarge,
            color = colors.accent,
            modifier = Modifier.clickable(onClick = onBack).padding(vertical = 8.dp),
        )
        Text(title, style = MaterialTheme.typography.headlineSmall, color = colors.primaryText)

        LazyColumn(Modifier.weight(1f).padding(top = 8.dp), state = listState) {
            items(state.messages, key = { it.id }) { m ->
                MessageBubble(m, mine = m.sender_id == me)
            }
        }

        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = {
                    Text(stringResource(R.string.type_message), style = MaterialTheme.typography.bodyLarge, color = colors.mutedText)
                },
                maxLines = 4,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    vm.send(draft)
                    draft = ""
                },
                enabled = draft.isNotBlank(),
            ) {
                Text(stringResource(R.string.send), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun MessageBubble(m: Message, mine: Boolean) {
    val colors = LocalSwiftsColors.current
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(if (mine) colors.accent else colors.neutralPill)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                m.body,
                style = MaterialTheme.typography.bodyLarge,
                color = if (mine) Color.White else colors.primaryText,
            )
        }
    }
}
