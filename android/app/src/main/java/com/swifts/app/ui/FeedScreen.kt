package com.swifts.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FeedScreen(vm: FeedViewModel) {
    val state by vm.state.collectAsState()
    when {
        state.loading -> Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) { CircularProgressIndicator() }

        state.error != null -> Text(state.error!!, modifier = Modifier.padding(16.dp))

        else -> LazyColumn(contentPadding = PaddingValues(12.dp)) {
            items(state.items, key = { it.id }) { post ->
                Card(Modifier.padding(vertical = 6.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(post.body, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}
