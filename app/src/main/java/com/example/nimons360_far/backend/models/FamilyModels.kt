package com.example.nimons360_far.backend.models

import com.google.gson.annotations.SerializedName

// API Response Model - List Of Families
data class FamilyResponse(
    @SerializedName("data") val data: List<Family>
)

// API Response Model - Single Family Detail
data class FamilyDetailResponse(
    @SerializedName("data") val data: Family
)

// API Response Model - Family Entity
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

// API Response Model - Family Member Entity
data class Member(
    val id: Int? = null,
    val fullName: String,
    val email: String,
    val joinedAt: String? = null,
    val profileImageUrl: String? = null
)

// API Request Model - Payload Create Family
data class CreateFamilyRequest(
    val name: String,
    val iconUrl: String
)

// API Request Model - Payload Join Family
data class JoinFamilyRequest(
    val familyId: Int,
    val familyCode: String
)

// API Request Model - Payload Leave Family
data class LeaveFamilyRequest(
    val familyId: Int
)