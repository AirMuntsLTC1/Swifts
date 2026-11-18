package com.swifts.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.swifts.app.R
import com.swifts.app.data.MAX_ATTACHMENT_TOTAL_BYTES
import com.swifts.app.data.PickedMedia
import com.swifts.app.data.readPickedMedia
import com.swifts.app.ui.theme.LocalSwiftsColors
import kotlinx.coroutines.launch

const val MAX_POST_LENGTH = 5000
private const val MAX_ATTACHMENTS = 4

/** A picked file shown as a thumbnail. Its bytes are kept in memory until the post is published. */
private class Attachment(val uri: Uri, val media: PickedMedia)

@Composable
fun ComposerDialog(
    posting: Boolean,
    onDismiss: () -> Unit,
    onPublish: (String, List<PickedMedia>) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    // Not saveable: the bytes are too large to store in saved state.
    var attachments by remember { mutableStateOf<List<Attachment>>(emptyList()) }
    var reading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MAX_ATTACHMENTS),
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                reading = true
                error = null
                val added = mutableListOf<Attachment>()
                var failed = false
                var tooBig = false
                for (uri in uris.take(MAX_ATTACHMENTS - attachments.size)) {
                    val media = readPickedMedia(context, uri).getOrNull()
                    if (media == null) {
                        failed = true
                        continue
                    }
                    val total = (attachments + added).sumOf { it.media.bytes.size.toLong() } + media.bytes.size
                    if (total > MAX_ATTACHMENT_TOTAL_BYTES) {
                        tooBig = true
                        continue
                    }
                    added += Attachment(uri, media)
                }
                attachments = attachments + added
                error = when {
                    tooBig -> context.getString(R.string.attach_too_large)
                    failed -> context.getString(R.string.attach_failed)
                    else -> null
                }
                reading = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_post)) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= MAX_POST_LENGTH) text = it },
                    placeholder = { Text(stringResource(R.string.whats_on_your_mind)) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (attachments.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        attachments.forEachIndexed { index, a ->
                            AttachmentThumb(
                                attachment = a,
                                onRemove = { attachments = attachments.filterIndexed { i, _ -> i != index } },
                            )
                        }
                    }
                }
                error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                TextButton(
                    enabled = !posting && !reading && attachments.size < MAX_ATTACHMENTS,
                    onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                    },
                ) {
                    Text(
                        stringResource(if (reading) R.string.attach_adding else R.string.add_media),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onPublish(text, attachments.map { it.media }) },
                enabled = !posting && !reading && (text.isNotBlank() || attachments.isNotEmpty()),
            ) { Text(stringResource(R.string.publish)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

/** Small preview of one attachment. Tap it to remove it before publishing. */
@Composable
private fun AttachmentThumb(attachment: Attachment, onRemove: () -> Unit) {
    val colors = LocalSwiftsColors.current
    Box(
        Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.neutralPill)
            .clickable(onClick = onRemove),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        if (attachment.media.kind == "image") {
            AsyncImage(
                model = attachment.uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(stringResource(R.string.video_label), style = MaterialTheme.typography.labelSmall, color = colors.primaryText)
        }
    }
}
