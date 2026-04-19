package com.example.nimons360_far.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nimons360_far.backend.models.LoginRequest
import com.example.nimons360_far.backend.network.ApiClient
import com.example.nimons360_far.backend.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authApi = ApiClient.getAuthApi(application)
    private val sessionManager = SessionManager(application)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun login(request: LoginRequest) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val response = authApi.login(request)
                if (response.isSuccessful && response.body() != null) {
                    val authData = response.body()!!.data
                    // Store Token and User Data
                    sessionManager.saveAuthToken(authData.token)
                    sessionManager.saveUserData(authData.user.fullName, authData.user.email)
                    _authState.value = AuthState.Success
                } else {
                    // check for invalid logins
                    if (response.code() == 401) {
                        _authState.value = AuthState.Error("Invalid email or password")
                    } else {
                        // Json parse the error messages
                        var parsedMessage: String? = null
                        val errorBodyString = response.errorBody()?.string()
                        if (!errorBodyString.isNullOrEmpty()) {
                            try {
                                val jsonObject = JSONObject(errorBodyString)
                                val errorObj = jsonObject.optJSONObject("error")
                                if (errorObj != null && errorObj.has("message")) {
                                    parsedMessage = errorObj.getString("message")
                                }
                            } catch (e: Exception) {}
                        }

                        val finalMsg =
                                parsedMessage
                                        ?: response.message().takeIf { it.isNotBlank() }
                                                ?: "Unknown error"
                        _authState.value = AuthState.Error("$finalMsg")
                    }
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Network error occurred")
            }
        }
    }
}
