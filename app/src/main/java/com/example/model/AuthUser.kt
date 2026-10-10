package com.example.model

data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String? = null,
    val isGoogleAccount: Boolean = false
)
