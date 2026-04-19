package com.example.nimons360_far.backend.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nimons360_far.backend.api.FamilyApi
import com.example.nimons360_far.backend.models.Family
import com.example.nimons360_far.backend.models.JoinFamilyRequest
import com.example.nimons360_far.backend.models.LeaveFamilyRequest
import com.example.nimons360_far.backend.network.AuthInterceptor
import com.example.nimons360_far.backend.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Sealed Class - UI Triggers (Toast & Navigation)
sealed class DetailEvent {
    data class ShowToast(val message: String) : DetailEvent()
    object NavigateBack : DetailEvent()
}

// Main Class - Families View Model
class FamilyDetailViewModel(application: Application) : AndroidViewModel(application) {

    // Initialize Dependencies
    private val sessionManager = SessionManager(application)
    private val token = sessionManager.fetchAuthToken() ?: ""
    private val bearerToken = "Bearer $token"
    val userEmail = sessionManager.fetchUserEmail() ?: ""

    // Lazy API Initialization With Auth Interceptor
    private val api: FamilyApi by lazy {
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(application, sessionManager))
            .build()
        Retrofit.Builder()
            .baseUrl("https://mad.labpro.hmif.dev")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FamilyApi::class.java)
    }

    // UI State Flow - Family Data
    private val _familyData = MutableStateFlow<Family?>(null)
    val familyData: StateFlow<Family?> = _familyData.asStateFlow()

    // UI State Flow - Loading Status
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // UI State Flow - Join Family Error Message
    private val _joinErrorMessage = MutableStateFlow<String?>(null)
    val joinErrorMessage: StateFlow<String?> = _joinErrorMessage.asStateFlow()

    // UI State Flow - Events
    private val _events = MutableSharedFlow<DetailEvent>()
    val events: SharedFlow<DetailEvent> = _events

    // Function - Load Family Detail
    fun loadFamilyDetail(familyId: Int) {
        if (familyId != 0) {
            // Set Loading State On
            _isLoading.value = true
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    // Try Execute Load Family Detail
                    val response = api.getFamilyDetail(bearerToken, familyId)
                    if (response.isSuccessful) {
                        // Execution Success
                        _familyData.value = response.body()?.data
                    } else {
                        // Execution Failed
                        _events.emit(DetailEvent.ShowToast("Failed to load family data"))
                    }
                } catch (e: Exception) {
                    // Error Handling - Connection Error
                    _events.emit(DetailEvent.ShowToast("Connection error"))
                } finally {
                    // Set Loading State Off
                    _isLoading.value = false
                }
            }
        }
    }

    // Function - Join Family
    fun joinFamily(familyId: Int, joinCode: String, onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Try Execute Join Family
                val res = api.joinFamily(bearerToken, JoinFamilyRequest(familyId, joinCode))
                if (res.isSuccessful) {
                    // Execution Success
                    _joinErrorMessage.value = null
                    onSuccess()
                    loadFamilyDetail(familyId)
                } else {
                    // Execution Failed - Wrong Family Code
                    _joinErrorMessage.value = "Wrong Family Code!"
                }
            } catch (e: Exception) {
                // Error Handling - Connection Error
                _joinErrorMessage.value = "Connection Error"
            }
        }
    }

    // Function - Leave Family
    fun leaveFamily(familyId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Try Execute Load Family
                val res = api.leaveFamily(token, LeaveFamilyRequest(familyId))
                if (res.isSuccessful) {
                    // Execution Success
                    _events.emit(DetailEvent.NavigateBack)
                } else {
                    // Execution Failed
                    _events.emit(DetailEvent.ShowToast("Failed to leave family"))
                }
            } catch (e: Exception) {
                // Error Handling - Connection Error
                _events.emit(DetailEvent.ShowToast("Connection Error"))
            }
        }
    }

    // Function - Helper Reset Join Family Error Message
    fun resetJoinError() {
        _joinErrorMessage.value = null
    }
}