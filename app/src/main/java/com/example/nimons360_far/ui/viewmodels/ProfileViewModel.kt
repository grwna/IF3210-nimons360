package com.example.nimons360_far.ui.viewmodels

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nimons360_far.backend.models.UserProfileData
import com.example.nimons360_far.backend.utils.UserRepository
import com.example.nimons360_far.backend.network.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import coil.imageLoader

sealed class ProfileState {
    object Idle : ProfileState()
    object Loading : ProfileState()
    data class Success(val profile: UserProfileData) : ProfileState()
    data class Error(val message: String) : ProfileState()
}

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository = UserRepository.getInstance(application)
    private val userApi = ApiClient.getUserApi(application)

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Idle)
    val profileState: StateFlow<ProfileState> = _profileState

    init {
        viewModelScope.launch {
            userRepository.userProfile.collect { data ->
                if (data != null) {
                    _profileState.value = ProfileState.Success(data)
                }
            }
        }
    }

    fun fetchProfile(forceRefresh: Boolean = false) {
        if (_profileState.value is ProfileState.Loading) return
        
        // Only show loading if there's no data
        if (_profileState.value !is ProfileState.Success) {
            _profileState.value = ProfileState.Loading
        }
        
        viewModelScope.launch {
            val result = userRepository.fetchProfile(forceRefresh)
            if (result.isSuccess) {
                _profileState.value = ProfileState.Success(result.getOrThrow())
            } else if (result.isFailure) {
                Log.e("ProfileViewModel", "Failed to fetch profile", result.exceptionOrNull())
                _profileState.value = ProfileState.Error("Failed to load profile. Please try again.")
            }
        }
    }

    fun updateProfile(fullName: String) {
        _profileState.value = ProfileState.Loading
        viewModelScope.launch {
            val result = userRepository.updateProfile(fullName)
            if (result.isSuccess) {
                _profileState.value = ProfileState.Success(result.getOrThrow())
            } else if (result.isFailure) {
                Log.e("ProfileViewModel", "Failed to update profile", result.exceptionOrNull())
                _profileState.value = ProfileState.Error("Failed to update profile. Please try again.")
            }
        }
    }

    fun uploadProfilePhoto(uri: Uri, context: android.content.Context) {
        _profileState.value = ProfileState.Loading
        viewModelScope.launch {
            try {
                val (response, tempFile) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val originalBitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()

                    // Image processing
                    val tempFile = File(context.cacheDir, "temp_profile_upload.jpg")
                    val outputStream = FileOutputStream(tempFile)
                    originalBitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
                    outputStream.close()
                    originalBitmap.recycle()

                    val requestFile = tempFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    val body = MultipartBody.Part.createFormData("photo", tempFile.name, requestFile)
                    val resp = userApi.uploadProfilePhoto(body)
                    Pair(resp, tempFile)
                }

                if (response.isSuccessful && response.body() != null) {
                    val newData = response.body()!!.data
                    
                    // Invalidate cache to reload profile picture
                    val rawUrl = newData.profileImageUrl
                    val finalUrl = if (rawUrl?.startsWith("/") == true) {
                        "https://mad.labpro.hmif.dev$rawUrl"
                    } else {
                        rawUrl
                    }
                    if (!finalUrl.isNullOrEmpty()) {
                        try {
                            val imageLoader = context.imageLoader
                            imageLoader.memoryCache?.remove(coil.memory.MemoryCache.Key(finalUrl))
                            imageLoader.diskCache?.remove(finalUrl)
                        } catch (e: Exception) {
                        }
                    }

                    // update profile data elsewhere in the app
                    userRepository.updateLocalProfile(newData)
                    _profileState.value = ProfileState.Success(newData)
                    tempFile.delete()
                } else {
                    val errorMsg = try {
                        val json = org.json.JSONObject(response.errorBody()?.string() ?: "")
                        json.getString("message")
                    } catch (e: Exception) {
                        if (response.code() == 413) "File size is too big (Max 500 KB)" else "Failed to upload photo"
                    }
                    _profileState.value = ProfileState.Error(errorMsg)
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Failed to upload photo", e)
                _profileState.value = ProfileState.Error("Failed to upload photo. Please try again.")
            }
        }
    }
}
