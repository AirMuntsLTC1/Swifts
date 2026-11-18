package com.swifts.app.domain

interface MediaStorage {
    suspend fun upload(bucket: String, path: String, bytes: ByteArray, mime: String): Result<Unit>
    fun publicUrl(bucket: String, path: String): String
}
