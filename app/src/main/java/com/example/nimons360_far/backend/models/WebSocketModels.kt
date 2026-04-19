package com.example.nimons360_far.backend.models

data class PresenceRequest(
    val type: String = "update_presence",
    val payload: PresencePayload,
    val timestamp: String
)

data class PresencePayload(
    val name: String?,
    val latitude: Double,
    val longitude: Double,
    val rotation: Float,
    val batteryLevel: Int,
    val isCharging: Boolean,
    val internetStatus: String, // "wifi" atau "mobile"
    val metadata: Map<String, Any> = emptyMap()
)

data class MemberPresenceResponse(
    val type: String,
    val payload: MemberPayload,
    val timestamp: String
)

data class MemberPayload(
    val userId: Int,
    val email: String,
    val fullName: String,
    val latitude: Double,
    val longitude: Double,
    val rotation: Float,
    val batteryLevel: Int,
    val isCharging: Boolean,
    val internetStatus: String,
    val metadata: Map<String, Any>
)