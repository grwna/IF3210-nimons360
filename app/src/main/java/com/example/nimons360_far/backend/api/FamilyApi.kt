package com.example.nimons360_far.backend.api

import com.example.nimons360_far.backend.models.CreateFamilyRequest
import com.example.nimons360_far.backend.models.FamilyDetailResponse
import com.example.nimons360_far.backend.models.FamilyResponse
import com.example.nimons360_far.backend.models.JoinFamilyRequest
import com.example.nimons360_far.backend.models.LeaveFamilyRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

// Retrofit Interface For Backend API Endpoints
interface FamilyApi {
    // GET - Fetch All Families
    @GET("/api/families")
    suspend fun getAllFamilies(
        @Header("Authorization") token: String
    ): Response<FamilyResponse>

    // GET - Fetch My Families
    @GET("/api/me/families")
    suspend fun getMyFamilies(
        @Header("Authorization") token: String
    ): Response<FamilyResponse>

    // GET - Fetch Family Detail
    @GET("/api/families/{familyId}")
    suspend fun getFamilyDetail(
        @Header("Authorization") token: String,
        @Path("familyId") familyId: Int
    ): Response<FamilyDetailResponse>

    // POST - Create Family
    @POST("/api/families")
    suspend fun createFamily(
        @Header("Authorization") token: String,
        @Body request: CreateFamilyRequest
    ): Response<FamilyDetailResponse>

    // POST - Join Family
    @POST("/api/families/join")
    suspend fun joinFamily(
        @Header("Authorization") token: String,
        @Body request: JoinFamilyRequest
    ): Response<Any>

    // POST - Join Family
    @POST("/api/families/leave")
    suspend fun leaveFamily(
        @Header("Authorization") token: String,
        @Body request: LeaveFamilyRequest
    ): Response<Any>

    // GET - Fetch 5 Random Families
    @GET("/api/families/discover")
    suspend fun getDiscoverFamilies(
        @Header("Authorization") token: String
    ): Response<FamilyResponse>
}