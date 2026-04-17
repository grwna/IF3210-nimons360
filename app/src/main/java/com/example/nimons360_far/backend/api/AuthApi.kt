package com.example.nimons360_far.backend.api

import com.example.nimons360_far.backend.models.LoginRequest
import com.example.nimons360_far.backend.models.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("/api/login") suspend fun login(@Body body: LoginRequest): Response<LoginResponse>
}
