package com.example.nimons360_far.ui.main

import android.os.Bundle
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

sealed class BottomNavItem(val title: String, val icon: ImageVector) {
    object Home : BottomNavItem("Home", Icons.Default.Home)
    object Map : BottomNavItem("Map", Icons.Default.LocationOn)
    object Families : BottomNavItem("Families", Icons.Default.Group)
}

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
                TopAppBar(
                    modifier = Modifier.shadow(
                        elevation = 8.dp,
                        spotColor = Color.Black,
                        ambientColor = Color.Black
                    ),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    title = {
                        val currentTitle =
                            if (selectedItem == BottomNavItem.Home) "Nimons360" else selectedItem.title
                        Text(
                            text = currentTitle,
                            fontWeight = if (currentTitle == "Nimons360") FontWeight.Bold else FontWeight.Normal,
                            fontSize = 22.sp
                        )
                    },
                    actions = {
                        IconButton(onClick = {/* TODO: Navigasi ke Profile Page*/ }) {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = "Profile",
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFFFFFFFF),

                modifier = Modifier.drawBehind {
                    val strokeWidth = 1.dp.toPx()
                    drawLine(
                        color = Color(0xFFCAC4D0),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = strokeWidth
                    )
                }
            ) {
                val items = listOf(BottomNavItem.Home, BottomNavItem.Map, BottomNavItem.Families)
                items.forEach { item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = item.title,
                                fontWeight = if (selectedItem == item) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        selected = selectedItem == item,
                        onClick = { mainViewModel.selectedItem = item },

                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF1565C0),
                            selectedTextColor = Color(0xFF1C1B1F),

                            unselectedIconColor = Color(0xFF49454F),
                            unselectedTextColor = Color(0xFF49454F),

                            indicatorColor = Color(0xFFD9E9FF)
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedItem != BottomNavItem.Map) {
                FloatingActionButton(
                    onClick = {
                        context?.supportFragmentManager?.beginTransaction()
                            ?.replace(R.id.fragment_container, CreateFamily())
                            ?.addToBackStack(null)
                            ?.commit()
                    },
                    containerColor = Color(0xFFEAF2FF),
                    contentColor = Color(0xFF1868D8),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Family"
                    )
                }
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