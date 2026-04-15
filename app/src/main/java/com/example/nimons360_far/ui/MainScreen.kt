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

sealed class BottomNavItem(val title: String, val icon: ImageVector) {
    object Home : BottomNavItem("Home", Icons.Default.Home)
    object Map : BottomNavItem("Map", Icons.Default.LocationOn)
    object Families : BottomNavItem("Families", Icons.Default.Group)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var selectedItem by remember { mutableStateOf<BottomNavItem>(BottomNavItem.Home) }

    Scaffold(
        topBar = {
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
                    val currentTitle = if (selectedItem == BottomNavItem.Home) "Nimons360" else selectedItem.title
                    Text(
                        text = currentTitle,
                        fontWeight = if (currentTitle == "Nimons360") FontWeight.Bold else FontWeight.Normal,
                        fontSize = 22.sp
                    )
                },
                actions = {
                    IconButton(onClick = {/* TODO: Navigasi ke Profile Page*/}) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = "Profile",
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            )
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
                        onClick = { selectedItem = item },

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
            FloatingActionButton(
                onClick = {/* TODO: Navigasi ke Form Create Family */},
                containerColor = Color(0xFFE3EEFF),
            ) {
                Icon(Icons.Default.Add,
                    contentDescription = "Add",
                    tint = Color(0xFF1565C0)
                )
            }
        }
    ) { innerPadding ->

        val modifier = Modifier.padding(innerPadding)

        when (selectedItem) {
            is BottomNavItem.Home -> HomeScreen(modifier = modifier)

            is BottomNavItem.Map -> MapScreen(modifier = modifier)

            is BottomNavItem.Families -> FamiliesScreen(modifier = modifier)
        }
    }
}