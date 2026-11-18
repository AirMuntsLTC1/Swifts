package com.swifts.app.data

import com.swifts.app.domain.Profile
import com.swifts.app.domain.ProfileRepository
import com.swifts.app.domain.ProfileUpdate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.URLEncoder

private const val PROFILE_COLUMNS =
    "id,username,display_name,avatar_url,bio,city,hometown,work,school"

class SupabaseProfileRepository(private val api: SupabaseClient) : ProfileRepository {

    @Serializable
    private data class FollowRow(val followee_id: String)

    override suspend fun profile(userId: String): Result<Profile> = runCatching {
        val text = api.request("GET", "/rest/v1/profiles?select=$PROFILE_COLUMNS&id=eq.${enc(userId)}&limit=1")
        api.json.decodeFromString<List<Profile>>(text).first()
    }

    override suspend fun update(userId: String, update: ProfileUpdate): Result<Profile> = runCatching {
        val body = api.json.encodeToString(ProfileUpdate.serializer(), update)
        val text = api.request(
            "PATCH",
            "/rest/v1/profiles?id=eq.${enc(userId)}&select=$PROFILE_COLUMNS",
            body,
            prefer = "return=representation",
        )
        api.json.decodeFromString<List<Profile>>(text).first()
    }

    override suspend fun setAvatar(userId: String, url: String): Result<Profile> = runCatching {
        val payload = buildJsonObject { put("avatar_url", url) }.toString()
        api.json.decodeFromString<List<Profile>>(
            api.request(
                "PATCH",
                "/rest/v1/profiles?id=eq.${enc(userId)}&select=$PROFILE_COLUMNS",
                payload,
                prefer = "return=representation",
            )
        ).first()
    }

    override suspend fun search(query: String): Result<List<Profile>> = runCatching {
        // Keep only characters that are safe inside the PostgREST filter.
        val q = query.trim().filter { it.isLetterOrDigit() || it == '_' || it == '.' }
        if (q.isEmpty()) return@runCatching emptyList()
        val filter = "or=(username.ilike.*${enc(q)}*,display_name.ilike.*${enc(q)}*)"
        api.json.decodeFromString<List<Profile>>(
            api.request("GET", "/rest/v1/profiles?select=$PROFILE_COLUMNS&$filter&limit=25")
        )
    }

    override suspend fun followingIds(userId: String): Result<Set<String>> = runCatching {
        val text = api.request("GET", "/rest/v1/follows?select=followee_id&follower_id=eq.${enc(userId)}")
        api.json.decodeFromString<List<FollowRow>>(text).map { it.followee_id }.toSet()
    }

    override suspend fun setFollowing(followerId: String, followeeId: String, follow: Boolean): Result<Unit> =
        runCatching {
            if (follow) {
                val body = buildJsonObject {
                    put("follower_id", followerId)
                    put("followee_id", followeeId)
                }.toString()
                api.request("POST", "/rest/v1/follows", body, prefer = "resolution=ignore-duplicates,return=minimal")
            } else {
                api.request(
                    "DELETE",
                    "/rest/v1/follows?follower_id=eq.${enc(followerId)}&followee_id=eq.${enc(followeeId)}",
                )
            }
            Unit
        }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
}

