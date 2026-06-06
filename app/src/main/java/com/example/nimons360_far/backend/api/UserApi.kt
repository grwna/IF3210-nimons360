package com.example.nimons360_far.backend.api

import com.example.nimons360_far.backend.models.UpdateProfileRequest
import com.example.nimons360_far.backend.models.UserProfileResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part

interface UserApi {
    @GET("/api/me")
    suspend fun getUserProfile(): Response<UserProfileResponse>

    @PATCH("/api/me")
    suspend fun updateUserProfile(@Body body: UpdateProfileRequest): Response<UserProfileResponse>

    @Multipart
    @POST("/api/me/photo")
    suspend fun uploadProfilePhoto(@Part photo: MultipartBody.Part): Response<UserProfileResponse>
}
