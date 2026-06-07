package com.example.nimons360_far.backend.utils

import android.content.Context
import com.example.nimons360_far.backend.models.UpdateProfileRequest
import com.example.nimons360_far.backend.models.UserProfileData
import com.example.nimons360_far.backend.network.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Repository for User profile data with caching 
class UserRepository private constructor(context: Context) {
    private val userApi = ApiClient.getUserApi(context)
    private val sessionManager = SessionManager(context)
    
    private val _userProfile = MutableStateFlow<UserProfileData?>(null)
    val userProfile: StateFlow<UserProfileData?> = _userProfile.asStateFlow()


    suspend fun fetchProfile(forceRefresh: Boolean = false): Result<UserProfileData> {
        if (!forceRefresh && _userProfile.value != null) {
            return Result.success(_userProfile.value!!)
        }

        return try {
            val response = userApi.getUserProfile()
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!.data
                _userProfile.value = data
                Result.success(data)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to fetch profile"))
            }
        } catch (e: java.io.IOException) {
            Result.failure(Exception("No internet connection"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun updateProfile(fullName: String): Result<UserProfileData> {
        return try {
            val request = UpdateProfileRequest(fullName = fullName)
            val response = userApi.updateUserProfile(request)
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!.data
                _userProfile.value = data
                // update cache
                sessionManager.saveUserData(data.fullName, data.email)
                Result.success(data)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to update profile"))
            }
        } catch (e: java.io.IOException) {
            Result.failure(Exception("No internet connection"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // update profile for other pages
    fun updateLocalProfile(data: UserProfileData) {
        _userProfile.value = data
        sessionManager.saveUserData(data.fullName, data.email)
    }

    fun clearCache() {
        _userProfile.value = null
    }

    companion object {
        @Volatile
        private var instance: UserRepository? = null

        fun getInstance(context: Context): UserRepository {
            return instance ?: synchronized(this) {
                instance ?: UserRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
