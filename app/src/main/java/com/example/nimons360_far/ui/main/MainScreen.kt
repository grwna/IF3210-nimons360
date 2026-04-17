package com.example.nimons360_far.ui.main

import android.os.Bundle
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.appcompat.app.AppCompatActivity
import com.example.nimons360_far.R
import com.example.nimons360_far.ui.families.FamiliesScreen
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.nimons360_far.ui.families.CreateFamily
import com.example.nimons360_far.ui.families.FamilyDetail
import com.example.nimons360_far.ui.home.HomeScreen
import com.example.nimons360_far.ui.map.MapScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nimons360_far.backend.viewmodels.MainViewModel
import com.example.nimons360_far.ui.components.HeadBar
import com.example.nimons360_far.ui.components.NavBar
import com.example.nimons360_far.ui.components.BottomNavItem
import com.example.nimons360_far.ui.components.FloatingFamilies

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val mainViewModel: MainViewModel = viewModel(
        viewModelStoreOwner = LocalContext.current as AppCompatActivity
    )
    val selectedItem = mainViewModel.selectedItem
    val context = LocalContext.current as? AppCompatActivity

    Scaffold(
        topBar = {
            if (selectedItem != BottomNavItem.Families) {
                HeadBar(
                    currentTitle = selectedItem.title,
                    onProfileClick = { /*TODO: Navigasi ke Form Profile */ },
                    selectedItem = selectedItem
                )
            }
        },
        bottomBar = {
            NavBar(
                selectedItem = selectedItem,
                onItemSelected = { mainViewModel.selectedItem = it }
            )
        },
        floatingActionButton = {
            if (selectedItem != BottomNavItem.Map) {
                FloatingFamilies(
                    onClick = {
                        context?.supportFragmentManager?.beginTransaction()
                            ?.replace(R.id.fragment_container, CreateFamily())
                            ?.addToBackStack(null)
                            ?.commit()
                    }
                )
            }
        }
    ) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)
        when (selectedItem) {
            is BottomNavItem.Home -> HomeScreen(modifier = modifier)
            is BottomNavItem.Map -> MapScreen(modifier = modifier)
            is BottomNavItem.Families -> {
                FamiliesScreen(
                    onNavigateToCreate = {},
                    onNavigateToDetail = { familyId ->
                        val familyDetail = FamilyDetail().apply {
                            arguments = Bundle().apply { putInt("familyId", familyId) }
                        }
                        context?.supportFragmentManager?.beginTransaction()
                            ?.replace(R.id.fragment_container, familyDetail)
                            ?.addToBackStack(null)
                            ?.commit()
                    }
                )
            }
        }
    }
}