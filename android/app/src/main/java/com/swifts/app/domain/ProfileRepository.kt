package com.swifts.app.domain

interface ProfileRepository {
    suspend fun profile(userId: String): Result<Profile>
    suspend fun update(userId: String, update: ProfileUpdate): Result<Profile>
    suspend fun setAvatar(userId: String, url: String): Result<Profile>
    suspend fun search(query: String): Result<List<Profile>>
    suspend fun followingIds(userId: String): Result<Set<String>>
    suspend fun setFollowing(followerId: String, followeeId: String, follow: Boolean): Result<Unit>
}
