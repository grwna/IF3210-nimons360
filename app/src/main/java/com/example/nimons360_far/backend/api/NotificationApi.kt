package com.example.nimons360_far.backend.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import com.example.nimons360_far.backend.models.SubscribeRequest
import com.example.nimons360_far.backend.models.SubscribeResponse
import com.example.nimons360_far.backend.models.UnsubscribeResponse
import com.example.nimons360_far.backend.models.SendBroadcastRequest
import com.example.nimons360_far.backend.models.SendBroadcastResponse
import com.example.nimons360_far.backend.models.SendGreetingRequest
import com.example.nimons360_far.backend.models.SendGreetingResponse

interface NotificationApi {
    @POST("/api/notifications/subscribe")
    suspend fun subscribe(@Body request: SubscribeRequest): Response<SubscribeResponse>

    @POST("/api/notifications/unsubscribe")
    suspend fun unsubscribe(): Response<UnsubscribeResponse>

    @POST("/api/notifications/send")
    suspend fun sendBroadcast(@Body request: SendBroadcastRequest): Response<SendBroadcastResponse>

    @POST("/api/notifications/greeting")
    suspend fun sendGreeting(@Body request: SendGreetingRequest): Response<SendGreetingResponse>
}
