package com.example.nimons360_far.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nimons360_far.backend.models.CreateFamilyRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Sealed Class - Handling One Time UI Events
sealed class CreateEvent {
    data class Success(val newFamilyId: Int, val familyName: String) : CreateEvent()
    data class Error(val message: String) : CreateEvent()
}

// Main Class - Create Family View Model
class CreateFamilyViewModel(application: Application) : AndroidViewModel(application) {
    // API Initialization
    private val api = com.example.nimons360_far.backend.network.ApiClient.getFamilyApi(application)

    // State Flow - Holding Selected Icon URL
    private val _selectedIconUrl = MutableStateFlow<String?>(null)
    val selectedIconUrl: StateFlow<String?> = _selectedIconUrl.asStateFlow()

    // Shared Flow - Emitting Success / Error Events To Fragment
    private val _events = MutableSharedFlow<CreateEvent>()
    val events: SharedFlow<CreateEvent> = _events

    // Function - Select Icon
    fun selectIcon(url: String) {
        _selectedIconUrl.value = url
    }

    // Function - Attempt Create Family
    fun attemptCreateFamily(familyName: String) {
        // Error Handling - Empty Family Name
        if (familyName.isEmpty()) {
            emitError("Family name cannot be empty!")
            return
        }

        // Error Handling - Empty Family Name
        if (familyName.length > 40) {
            emitError("Family name is too long!")
            return
        }

        // Error Handling - No Selected Icon
        val currentIcon = _selectedIconUrl.value
        if (currentIcon == null) {
            emitError("Please select an icon!")
            return
        }

        // Execute Network Request (Background Thread)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Send Request & Get Response
                val request = CreateFamilyRequest(familyName, currentIcon)
                val response = api.createFamily(request)
                if (response.isSuccessful && response.body() != null) {
                    // Execution Success
                    val newFamily = response.body()!!.data
                    _events.emit(CreateEvent.Success(newFamily.id, newFamily.name))
                } else {
                    // Execution Failed
                    val msg = if (response.code() == 401) "Token expired, please re-login" else "Failed to create family"
                    _events.emit(CreateEvent.Error(msg))
                }
            } catch (e: Exception) {
                // Error Handling - Connection Error
                _events.emit(CreateEvent.Error("Connection Error"))
            }
        }
    }

    // Function - Helper Emit Error Events
    private fun emitError(message: String) {
        viewModelScope.launch { _events.emit(CreateEvent.Error(message)) }
    }
}