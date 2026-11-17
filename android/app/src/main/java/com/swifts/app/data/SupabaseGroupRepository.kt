package com.swifts.app.data

import com.swifts.app.domain.Group
import com.swifts.app.domain.GroupRepository
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.URLEncoder

private const val GROUP_COLUMNS = "id,name,description,privacy,cover_url,created_by"

class SupabaseGroupRepository(private val api: SupabaseClient) : GroupRepository {

    @Serializable
    private data class MembershipRow(val group_id: String)

    override suspend fun listGroups(query: String): Result<List<Group>> = runCatching {
        val q = query.trim().filter { it.isLetterOrDigit() || it == '_' || it == '.' }
        val nameFilter = if (q.isEmpty()) "" else "&name=ilike.*${enc(q)}*"
        api.json.decodeFromString<List<Group>>(
            api.request(
                "GET",
                "/rest/v1/groups?select=$GROUP_COLUMNS&privacy=eq.public$nameFilter&order=created_at.desc&limit=30",
            )
        )
    }

    override suspend fun group(id: String): Result<Group> = runCatching {
        api.json.decodeFromString<List<Group>>(
            api.request("GET", "/rest/v1/groups?select=$GROUP_COLUMNS&id=eq.${enc(id)}&limit=1")
        ).first()
    }

    override suspend fun create(
        ownerId: String,
        name: String,
        description: String,
        privacy: String,
    ): Result<Group> = runCatching {
        val body = buildJsonObject {
            put("name", name)
            put("description", description)
            put("privacy", privacy)
            put("created_by", ownerId)
        }.toString()
        val text = api.request(
            "POST",
            "/rest/v1/groups?select=$GROUP_COLUMNS",
            body,
            prefer = "return=representation",
        )
        api.json.decodeFromString<List<Group>>(text).first()
    }

    override suspend fun joinedGroupIds(userId: String): Result<Set<String>> = runCatching {
        val text = api.request("GET", "/rest/v1/group_members?select=group_id&user_id=eq.${enc(userId)}")
        api.json.decodeFromString<List<MembershipRow>>(text).map { it.group_id }.toSet()
    }

    override suspend fun setJoined(groupId: String, userId: String, joined: Boolean): Result<Unit> = runCatching {
        if (joined) {
            val body = buildJsonObject { put("group_id", groupId); put("user_id", userId) }.toString()
            api.request("POST", "/rest/v1/group_members", body, prefer = "resolution=ignore-duplicates,return=minimal")
        } else {
            api.request(
                "DELETE",
                "/rest/v1/group_members?group_id=eq.${enc(groupId)}&user_id=eq.${enc(userId)}",
            )
        }
        Unit
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
}
