package com.example.nimons360_far.backend.models

data class UpdateProfileRequest(
    val fullName: String
)

data class UserProfileResponse(
    val data: UserProfileData
)

data class UserProfileData(
    val id: Int,
    val nim: String,
    val email: String,
    val fullName: String,
    val createdAt: String,
    val updatedAt: String
)
