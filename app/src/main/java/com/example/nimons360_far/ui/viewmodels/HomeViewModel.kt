package com.example.nimons360_far.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nimons360_far.backend.models.Family
import com.example.nimons360_far.backend.network.ApiClient
import com.example.nimons360_far.backend.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    private val token = sessionManager.fetchAuthToken() ?: ""
    private val familyApi = ApiClient.getFamilyApi(application)

    private val _myFamilies = MutableStateFlow<List<Family>>(emptyList())
    val myFamilies: StateFlow<List<Family>> = _myFamilies.asStateFlow()

    private val _discoverFamilies = MutableStateFlow<List<Family>>(emptyList())
    val discoverFamilies: StateFlow<List<Family>> = _discoverFamilies.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        refreshAll()
    }

    fun refreshAll() {
        if (token.isEmpty()) return
        
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val myRes = familyApi.getMyFamilies()
                val discRes = familyApi.getDiscoverFamilies()

                if (myRes.isSuccessful) {
                    _myFamilies.value = myRes.body()?.data ?: emptyList()
                } else {
                    _error.value = "Failed to load my families"
                }

                if (discRes.isSuccessful) {
                    _discoverFamilies.value = discRes.body()?.data ?: emptyList()
                } else {
                    _error.value = "Failed to load discovery families"
                }

            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "An error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
