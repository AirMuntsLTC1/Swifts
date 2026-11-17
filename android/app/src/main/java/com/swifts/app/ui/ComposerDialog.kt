package com.swifts.app.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.swifts.app.R

const val MAX_POST_LENGTH = 5000

@Composable
fun ComposerDialog(
    posting: Boolean,
    onDismiss: () -> Unit,
    onPublish: (String) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_post)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { if (it.length <= MAX_POST_LENGTH) text = it },
                placeholder = { Text(stringResource(R.string.whats_on_your_mind)) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onPublish(text) },
                enabled = !posting && text.isNotBlank(),
            ) { Text(stringResource(R.string.publish)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
