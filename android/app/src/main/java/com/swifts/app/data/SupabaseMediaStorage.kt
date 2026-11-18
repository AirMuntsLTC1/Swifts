package com.swifts.app.data

import com.swifts.app.domain.MediaStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class SupabaseMediaStorage(private val api: SupabaseClient) : MediaStorage {

    override suspend fun upload(bucket: String, path: String, bytes: ByteArray, mime: String): Result<Unit> =
        runCatching {
            withContext(Dispatchers.IO) {
                val token = api.accessToken() ?: error("Not signed in")
                val request = Request.Builder()
                    .url("${api.base}/storage/v1/object/$bucket/$path")
                    .header("apikey", api.anon)
                    .header("Authorization", "Bearer $token")
                    .post(bytes.toRequestBody(mime.toMediaType()))
                    .build()
                api.http.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) error("Upload failed: ${resp.code}")
                }
            }
        }

    override fun publicUrl(bucket: String, path: String): String =
        "${api.base}/storage/v1/object/public/$bucket/$path"
}
