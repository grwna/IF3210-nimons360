package com.example.nimons360_far.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nimons360_far.backend.data.AppDatabase
import com.example.nimons360_far.backend.data.PinnedFamily
import com.example.nimons360_far.backend.models.Family
import com.example.nimons360_far.backend.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Main Class - Families View Model
class FamiliesViewModel(application: Application) : AndroidViewModel(application) {
    // Initialize Dependencies
    private val sessionManager = SessionManager(application)
    private val database = AppDatabase.getDatabase(application)
    private val pinnedDao = database.pinnedFamilyDao()
    private val token = sessionManager.fetchAuthToken() ?: ""

    // API Initialization
    private val api = com.example.nimons360_far.backend.network.ApiClient.getFamilyApi(application)

    // UI State Flow - Fetched All Families
    private val _allFamilies = MutableStateFlow<List<Family>>(emptyList())
    val allFamilies: StateFlow<List<Family>> = _allFamilies.asStateFlow()

    // UI State Flow - Fetched My Families
    private val _myFamilies = MutableStateFlow<List<Family>>(emptyList())
    val myFamilies: StateFlow<List<Family>> = _myFamilies.asStateFlow()

    // UI State Flow - Fetched ID Pinned Families
    private val _pinnedIds = MutableStateFlow<Set<Int>>(emptySet())
    val pinnedIds: StateFlow<Set<Int>> = _pinnedIds.asStateFlow()

    // UI State Flow - Loading Status
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // UI State Flow - Fetched Filtered Families
    private val _filterType = MutableStateFlow("All")
    val filterType: StateFlow<String> = _filterType.asStateFlow()

    // UI States Flow - Fetched Searched Families
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Init - Fetch Families & Pinned Families
    init {
        fetchFamilies()
        observePinnedFamilies()
    }

    // Function - Fetch Families
    fun fetchFamilies() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Try Fetch Data
                if (token.isNotEmpty()) {
                    val allRes = api.getAllFamilies()
                    val myRes = api.getMyFamilies()

                    // Success Fetch All Families
                    if (allRes.isSuccessful) {
                        _allFamilies.value = allRes.body()?.data ?: emptyList()
                    }

                    // Success Fetch My Families
                    if (myRes.isSuccessful) {
                        _myFamilies.value = myRes.body()?.data ?: emptyList()
                    }
                }
            } catch (_: Exception) {
                // Silent Error Handling
            } finally {
                // Set Loading State Off
                _isLoading.value = false
            }
        }
    }

    // Function - Fetch Pinned Families (Listen Local Room Database)
    private fun observePinnedFamilies() {
        viewModelScope.launch {
            pinnedDao.getAllPinned().collect { pinnedList ->
                _pinnedIds.value = pinnedList.map { it.familyId }.toSet()
            }
        }
    }

    // Function - Helper Set Filter Type
    fun setFilterType(type: String) {
        _filterType.value = type
    }

    // Function - Helper Set Search Query
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Function - Helper Toggle Pin Family
    fun togglePin(familyId: Int, isPinned: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (isPinned) {
                pinnedDao.unpinFamily(familyId)
            } else {
                pinnedDao.pinFamily(PinnedFamily(familyId))
            }
        }
    }
}