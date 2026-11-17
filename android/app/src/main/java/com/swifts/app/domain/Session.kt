package com.swifts.app.domain

data class Session(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val expiresAtEpochSec: Long,
)
