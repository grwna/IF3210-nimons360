package com.example.nimons360_far.ui.viewmodels

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

sealed class DetailEvent {
    data class ShowToast(val message: String) : DetailEvent()
    object NavigateBack : DetailEvent()
}

class FamilyDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    val userEmail = sessionManager.fetchUserEmail() ?: ""

    private val api = com.example.nimons360_far.backend.network.ApiClient.getFamilyApi(application)

    private val _familyData = MutableStateFlow<Family?>(null)
    val familyData: StateFlow<Family?> = _familyData.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _joinErrorMessage = MutableStateFlow<String?>(null)
    val joinErrorMessage: StateFlow<String?> = _joinErrorMessage.asStateFlow()

    private val _events = MutableSharedFlow<DetailEvent>()
    val events: SharedFlow<DetailEvent> = _events

    fun loadFamilyDetail(familyId: Int) {
        if (familyId == 0) return
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = api.getFamilyDetail(familyId)
                if (response.isSuccessful) {
                    _familyData.value = response.body()?.data
                } else {
                    _events.emit(DetailEvent.ShowToast("Failed to load family data"))
                }
            } catch (e: Exception) {
                _events.emit(DetailEvent.ShowToast("Connection error"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun joinFamily(familyId: Int, joinCode: String, onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val res = api.joinFamily(JoinFamilyRequest(familyId, joinCode))
                if (res.isSuccessful) {
                    _joinErrorMessage.value = null
                    onSuccess()
                    loadFamilyDetail(familyId)
                } else {
                    _joinErrorMessage.value = "Wrong Family Code!"
                }
            } catch (e: Exception) {
                _joinErrorMessage.value = "Error connection"
            }
        }
    }

    fun leaveFamily(familyId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val res = api.leaveFamily(LeaveFamilyRequest(familyId))
                if (res.isSuccessful) {
                    _events.emit(DetailEvent.NavigateBack)
                } else {
                    _events.emit(DetailEvent.ShowToast("Failed to leave family"))
                }
            } catch (e: Exception) {
                _events.emit(DetailEvent.ShowToast("Error connection"))
            }
        }
    }

    fun resetJoinError() {
        _joinErrorMessage.value = null
    }
}