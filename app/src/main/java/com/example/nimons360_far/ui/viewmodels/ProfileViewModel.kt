package com.example.nimons360_far.ui.viewmodels

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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
                _profileState.value = ProfileState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
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
                _profileState.value = ProfileState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }

    fun uploadProfilePhoto(uri: Uri, context: android.content.Context) {
        _profileState.value = ProfileState.Loading
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                val tempFile = File(context.cacheDir, "temp_profile_upload.jpg")
                val outputStream = FileOutputStream(tempFile)
                bitmap?.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
                outputStream.close()
                val requestFile = tempFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("photo", tempFile.name, requestFile)
                val response = userApi.uploadProfilePhoto(body)
                if (response.isSuccessful && response.body() != null) {
                    _profileState.value = ProfileState.Success(response.body()!!.data)
                    tempFile.delete()
                } else {
                    _profileState.value = ProfileState.Error("Failed to Upload Photo : File Size is Too Big")
                }
            } catch (_: Exception) {
                _profileState.value = ProfileState.Error("Failed to Upload Photo")
            }
        }
    }
}
