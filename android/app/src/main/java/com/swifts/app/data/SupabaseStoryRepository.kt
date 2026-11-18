package com.swifts.app.data

import com.swifts.app.domain.Story
import com.swifts.app.domain.StoryRepository
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val STORY_SELECT =
    "id,author_id,media_path,created_at,author:profiles!stories_author_id_fkey(username,display_name,avatar_url)"

class SupabaseStoryRepository(private val api: SupabaseClient) : StoryRepository {

    override suspend fun visibleStories(authorIds: List<String>): Result<List<Story>> = runCatching {
        if (authorIds.isEmpty()) return@runCatching emptyList<Story>()
        val ids = authorIds.joinToString(",")
        api.json.decodeFromString<List<Story>>(
            api.request("GET", "/rest/v1/stories?select=$STORY_SELECT&author_id=in.($ids)&order=created_at.desc&limit=200")
        )
    }

    override suspend fun create(authorId: String, mediaPath: String): Result<Story> = runCatching {
        val body = buildJsonObject {
            put("author_id", authorId)
            put("media_path", mediaPath)
        }.toString()
        api.json.decodeFromString<List<Story>>(
            api.request(
                "POST",
                "/rest/v1/stories?select=id,author_id,media_path,created_at",
                body,
                prefer = "return=representation",
            )
        ).first()
    }
}
