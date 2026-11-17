package com.swifts.app.domain

interface GroupRepository {
    suspend fun listGroups(query: String): Result<List<Group>>
    suspend fun group(id: String): Result<Group>
    suspend fun create(ownerId: String, name: String, description: String, privacy: String): Result<Group>
    suspend fun joinedGroupIds(userId: String): Result<Set<String>>
    suspend fun setJoined(groupId: String, userId: String, joined: Boolean): Result<Unit>
}
