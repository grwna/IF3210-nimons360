package com.example.nimons360_far.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.nimons360_far.ui.components.BottomNavItem

class MainViewModel : ViewModel() {
    var selectedItem by mutableStateOf<BottomNavItem>(BottomNavItem.Home)
}