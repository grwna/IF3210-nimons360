package com.example.nimons360_far.backend.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nimons360_far.backend.api.FamilyApi
import com.example.nimons360_far.backend.models.CreateFamilyRequest
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

sealed class CreateEvent {
    data class Success(val newFamilyId: Int, val familyName: String) : CreateEvent()
    data class Error(val message: String) : CreateEvent()
}

class CreateFamilyViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
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

    private val _selectedIconUrl = MutableStateFlow<String?>(null)
    val selectedIconUrl: StateFlow<String?> = _selectedIconUrl.asStateFlow()

    private val _events = MutableSharedFlow<CreateEvent>()
    val events: SharedFlow<CreateEvent> = _events

    fun selectIcon(url: String) {
        _selectedIconUrl.value = url
    }

    fun attemptCreateFamily(familyName: String) {
        if (familyName.isEmpty()) {
            emitError("Family name cannot be empty!")
            return
        }
        if (familyName.length > 40) {
            emitError("Family name is too long!")
            return
        }
        val currentIcon = _selectedIconUrl.value
        if (currentIcon == null) {
            emitError("Please select an icon!")
            return
        }

        val token = sessionManager.fetchAuthToken() ?: ""
        val bearerToken = "Bearer $token"

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = CreateFamilyRequest(familyName, currentIcon)
                val response = api.createFamily(bearerToken, request)
                if (response.isSuccessful && response.body() != null) {
                    val newFamily = response.body()!!.data
                    _events.emit(CreateEvent.Success(newFamily.id, newFamily.name))
                } else {
                    val msg = if (response.code() == 401) "Token expired, please re-login" else "Failed to create family"
                    _events.emit(CreateEvent.Error(msg))
                }
            } catch (e: Exception) {
                _events.emit(CreateEvent.Error("Connection Error: ${e.message}"))
            }
        }
    }

    private fun emitError(message: String) {
        viewModelScope.launch { _events.emit(CreateEvent.Error(message)) }
    }
}