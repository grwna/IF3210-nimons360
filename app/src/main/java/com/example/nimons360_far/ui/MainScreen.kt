package com.example.nimons360_far.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.sp
import com.example.nimons360_far.ui.screen.FamiliesScreen
import com.example.nimons360_far.ui.screen.HomeScreen
import com.example.nimons360_far.ui.screen.MapScreen

import com.example.nimons360_far.ui.components.NavBar
import com.example.nimons360_far.ui.components.HeadBar
import com.example.nimons360_far.ui.components.BottomNavItem
import com.example.nimons360_far.ui.components.FloatingFamilies


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var selectedItem by remember { mutableStateOf<BottomNavItem>(BottomNavItem.Home) }

    Scaffold(
        topBar = {
            HeadBar(
                currentTitle = selectedItem.title,
                onProfileClick = { /*TODO: Navigasi ke Form Profile */ },
                selectedItem = selectedItem
            )
        },
        bottomBar = {
            NavBar(
                selectedItem = selectedItem,
                onItemSelected = { selectedItem = it }
            )
        },
        floatingActionButton = {
            FloatingFamilies(
                onClick = { /* TODO: Navigasi ke Form Create Family */ }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedItem) {
                is BottomNavItem.Home -> HomeScreen()
                is BottomNavItem.Map -> MapScreen()
                is BottomNavItem.Families -> FamiliesScreen()
            }
        }
    }
}