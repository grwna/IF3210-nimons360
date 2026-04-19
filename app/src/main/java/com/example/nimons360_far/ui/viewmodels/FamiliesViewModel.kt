package com.example.nimons360_far.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nimons360_far.backend.api.FamilyApi
import com.example.nimons360_far.backend.data.AppDatabase
import com.example.nimons360_far.backend.data.PinnedFamily
import com.example.nimons360_far.backend.models.Family
import com.example.nimons360_far.backend.network.AuthInterceptor
import com.example.nimons360_far.backend.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FamiliesViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    private val database = AppDatabase.getDatabase(application)
    private val pinnedDao = database.pinnedFamilyDao()
    private val token = sessionManager.fetchAuthToken() ?: ""

    private val api = com.example.nimons360_far.backend.network.ApiClient.getFamilyApi(application)

    private val _allFamilies = MutableStateFlow<List<Family>>(emptyList())
    val allFamilies: StateFlow<List<Family>> = _allFamilies.asStateFlow()

    private val _myFamilies = MutableStateFlow<List<Family>>(emptyList())
    val myFamilies: StateFlow<List<Family>> = _myFamilies.asStateFlow()

    private val _pinnedIds = MutableStateFlow<Set<Int>>(emptySet())
    val pinnedIds: StateFlow<Set<Int>> = _pinnedIds.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _filterType = MutableStateFlow("All")
    val filterType: StateFlow<String> = _filterType.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        fetchFamilies()
        observePinnedFamilies()
    }

    fun fetchFamilies() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (token.isNotEmpty()) {
                    val allRes = api.getAllFamilies()
                    val myRes = api.getMyFamilies()
                    if (allRes.isSuccessful) _allFamilies.value = allRes.body()?.data ?: emptyList()
                    if (myRes.isSuccessful) _myFamilies.value = myRes.body()?.data ?: emptyList()
                }
            } catch (e: Exception) {

            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun observePinnedFamilies() {
        viewModelScope.launch {
            pinnedDao.getAllPinned().collect { pinnedList ->
                _pinnedIds.value = pinnedList.map { it.familyId }.toSet()
            }
        }
    }

    fun setFilterType(type: String) {
        _filterType.value = type
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun togglePin(familyId: Int, isPinned: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (isPinned) pinnedDao.unpinFamily(familyId)
            else pinnedDao.pinFamily(PinnedFamily(familyId))
        }
    }
}