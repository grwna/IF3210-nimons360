package com.example.nimons360_far.backend.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nimons360_far.backend.models.UserProfileData
import com.example.nimons360_far.backend.utils.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ProfileState {
    object Idle : ProfileState()
    object Loading : ProfileState()
    data class Success(val profile: UserProfileData) : ProfileState()
    data class Error(val message: String) : ProfileState()
}

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository = UserRepository.getInstance(application)

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

    // Stub for bottom sheet integration
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
}
