package com.swifts.app.data

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/** Opens the native Google account chooser and returns the Google ID token. */
object GoogleCredentialSource {
    suspend fun fetchIdToken(activity: Activity, webClientId: String, hashedNonce: String): String {
        val option = GetSignInWithGoogleOption.Builder(webClientId)
            .setNonce(hashedNonce)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()
        val response = CredentialManager.create(activity).getCredential(activity, request)
        val credential = response.credential
        require(
            credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) { "Unexpected credential type" }
        return GoogleIdTokenCredential.createFrom(credential.data).idToken
    }
}
