package com.example.nimons360_far.backend.models

data class SubscribeRequest(
    val fcmToken: String
)

data class SubscribeResponse(
    val data: SubscribeData
)

data class SubscribeData(
    val subscribed: Boolean
)

data class UnsubscribeResponse(
    val data: UnsubscribeData
)

data class UnsubscribeData(
    val unsubscribed: Boolean
)

data class SendBroadcastRequest(
    val familyId: Int,
    val message: String
)

data class SendBroadcastResponse(
    val data: SendBroadcastData
)

data class SendBroadcastData(
    val sent: Boolean
)

data class SendGreetingRequest(
    val familyId: Int,
    val targetUserId: Int,
    val message: String
)

data class SendGreetingResponse(
    val data: SendGreetingData
)

data class SendGreetingData(
    val delivered: Boolean
)
