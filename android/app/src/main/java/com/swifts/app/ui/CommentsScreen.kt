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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.swifts.app.R
import com.swifts.app.domain.Comment
import com.swifts.app.ui.theme.LocalSwiftsColors

@Composable
fun CommentsDialog(services: AppServices, postId: String, postAuthorId: String, onClose: () -> Unit) {
    val vm: CommentsViewModel = viewModel(
        key = "comments-$postId",
        factory = viewModelFactory {
            initializer { CommentsViewModel(services.comments, services.auth, postId) }
        },
    )
    val state by vm.state.collectAsState()
    val colors = LocalSwiftsColors.current
    val me = services.auth.currentUserId().orEmpty()
    var draft by rememberSaveable { mutableStateOf("") }
    val tree = remember(state.comments, state.counts, state.liked) {
        buildCommentTree(state.comments, state.counts, state.liked)
    }
    // The most-hearted comment gets a "Top" label, once it has at least one heart.
    val bestId = tree.maxByOrNull { it.likes }?.takeIf { it.likes > 0 }?.comment?.id
    val replyTo = state.replyTo

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.comments_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.primaryText,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    stringResource(R.string.close),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.accent,
                    modifier = Modifier.clickable(onClick = onClose).padding(8.dp),
                )
            }

            when {
                state.loading -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                tree.isEmpty() -> Text(
                    stringResource(R.string.no_comments),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.mutedText,
                    modifier = Modifier.padding(top = 24.dp),
                )

                else -> LazyColumn(Modifier.weight(1f)) {
                    items(tree, key = { it.comment.id }) { node ->
                        CommentThread(
                            node = node,
                            postAuthorId = postAuthorId,
                            bestId = bestId,
                            me = me,
                            onLike = { vm.toggleLike(it) },
                            onReply = { rootId, name -> vm.setReply(rootId, name) },
                            onDelete = { vm.delete(it) },
                        )
                    }
                }
            }

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            if (replyTo != null) {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.replying_to, replyTo.username),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.mutedText,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        stringResource(R.string.cancel),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.accent,
                        modifier = Modifier.clickable { vm.setReply(null, "") },
                    )
                }
            }

            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = {
                        Text(stringResource(R.string.comment_hint), style = MaterialTheme.typography.bodyLarge, color = colors.mutedText)
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
                    enabled = draft.isNotBlank() && !state.posting,
                ) {
                    Text(stringResource(R.string.send), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

/** A top-level comment, followed by its replies indented underneath. */
@Composable
private fun CommentThread(
    node: CommentNode,
    postAuthorId: String,
    bestId: String?,
    me: String,
    onLike: (String) -> Unit,
    onReply: (String, String) -> Unit,
    onDelete: (String) -> Unit,
) {
    val root = node.comment
    CommentRow(
        comment = root,
        likes = node.likes,
        liked = node.liked,
        isCreator = root.author_id == postAuthorId,
        isBest = root.id == bestId,
        isMine = root.author_id == me,
        indent = 0.dp,
        onLike = { onLike(root.id) },
        onReply = { onReply(root.id, root.author?.username.orEmpty()) },
        onDelete = { onDelete(root.id) },
    )
    node.replies.forEach { r ->
        CommentRow(
            comment = r.comment,
            likes = r.likes,
            liked = r.liked,
            isCreator = r.comment.author_id == postAuthorId,
            isBest = false,
            isMine = r.comment.author_id == me,
            indent = 40.dp,
            onLike = { onLike(r.comment.id) },
            onReply = { onReply(root.id, r.comment.author?.username.orEmpty()) },
            onDelete = { onDelete(r.comment.id) },
        )
    }
}

@Composable
private fun CommentRow(
    comment: Comment,
    likes: Int,
    liked: Boolean,
    isCreator: Boolean,
    isBest: Boolean,
    isMine: Boolean,
    indent: Dp,
    onLike: () -> Unit,
    onReply: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = LocalSwiftsColors.current
    val name = comment.author?.let { it.display_name.ifBlank { it.username } } ?: stringResource(R.string.you)
    Row(
        Modifier.fillMaxWidth().padding(start = indent, top = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Avatar(comment.author?.avatar_url, name, size = 36.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "@${comment.author?.username ?: stringResource(R.string.you)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.primaryText,
                )
                if (isCreator) {
                    Spacer(Modifier.width(6.dp))
                    Badge(stringResource(R.string.creator))
                }
                if (isBest) {
                    Spacer(Modifier.width(6.dp))
                    Badge(stringResource(R.string.top_comment))
                }
            }
            Text(comment.body, style = MaterialTheme.typography.bodyLarge, color = colors.primaryText)
            Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.reply),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.mutedText,
                    modifier = Modifier.clickable(onClick = onReply).padding(vertical = 4.dp),
                )
                if (isMine) {
                    Spacer(Modifier.width(16.dp))
                    Text(
                        stringResource(R.string.delete),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.clickable(onClick = onDelete).padding(vertical = 4.dp),
                    )
                }
            }
        }
        // Heart on the right: tap to like, the number shows how many people liked it.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(start = 8.dp).clickable(onClick = onLike),
        ) {
            AssetIcon(
                name = if (liked) "ic_heart_filled" else "ic_heart",
                tint = if (liked) colors.accent else colors.mutedText,
                contentDescription = stringResource(R.string.like),
                size = 20.dp,
            )
            Text(likes.toString(), style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
        }
    }
}

@Composable
private fun Badge(label: String) {
    val colors = LocalSwiftsColors.current
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(colors.accentSoft)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.accent)
    }
}
