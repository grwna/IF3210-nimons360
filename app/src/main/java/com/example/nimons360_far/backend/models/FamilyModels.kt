package com.example.nimons360_far.backend.models

import com.google.gson.annotations.SerializedName

data class FamilyResponse(
    @SerializedName("data") val data: List<Family>
)

data class FamilyDetailResponse(
    @SerializedName("data") val data: Family
)

data class Family(
    val id: Int,
    val name: String,
    val iconUrl: String,
    val familyCode: String? = null,
    val isMember: Boolean? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val members: List<Member>? = null
)

data class Member(
    val id: Int? = null,
    val fullName: String,
    val email: String,
    val joinedAt: String? = null
)

data class CreateFamilyRequest(
    val name: String,
    val iconUrl: String
)

data class JoinFamilyRequest(
    val familyId: Int,
    val familyCode: String
)

data class LeaveFamilyRequest(
    val familyId: Int
)