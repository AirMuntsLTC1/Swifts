package com.swifts.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.swifts.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(vm: FeedViewModel, onSignOut: () -> Unit) {
    val state by vm.state.collectAsState()
    var showComposer by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feed_title)) },
                actions = {
                    TextButton(onClick = onSignOut) { Text(stringResource(R.string.sign_out)) }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showComposer = true }) { Text("+") }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                state.loading && state.items.isEmpty() ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))

                state.error != null && state.items.isEmpty() ->
                    Text(state.error!!, modifier = Modifier.padding(16.dp))

                else -> LazyColumn(contentPadding = PaddingValues(12.dp)) {
                    state.error?.let { err ->
                        item { Text(err, color = MaterialTheme.colorScheme.error) }
                    }
                    items(state.items, key = { it.id }) { post ->
                        val liked = post.id in state.liked
                        Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Text(post.body, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyLarge)
                            TextButton(
                                onClick = { vm.toggleLike(post.id) },
                                modifier = Modifier.padding(start = 4.dp),
                            ) {
                                Text(stringResource(if (liked) R.string.liked else R.string.like))
                            }
                        }
                    }
                }
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
