package com.swifts.app.ui

import com.swifts.app.domain.Comment

data class CommentNode(
    val comment: Comment,
    val likes: Int,
    val liked: Boolean,
    val replies: List<CommentNode>,
)

/** Top-level comments with their replies. Best first: most hearts, then oldest. */
fun buildCommentTree(
    comments: List<Comment>,
    likeCounts: Map<String, Int>,
    liked: Set<String>,
): List<CommentNode> {
    val order = Comparator<Comment> { a, b ->
        val la = likeCounts[a.id] ?: 0
        val lb = likeCounts[b.id] ?: 0
        if (la != lb) lb.compareTo(la) else a.created_at.compareTo(b.created_at)
    }
    val byParent = comments.groupBy { it.parent_id }

    fun node(c: Comment): CommentNode = CommentNode(
        comment = c,
        likes = likeCounts[c.id] ?: 0,
        liked = c.id in liked,
        replies = byParent[c.id].orEmpty().sortedWith(order).map { node(it) },
    )

    return byParent[null].orEmpty().sortedWith(order).map { node(it) }
}
