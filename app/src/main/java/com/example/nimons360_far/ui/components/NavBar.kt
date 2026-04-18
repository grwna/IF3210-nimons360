package com.example.nimons360_far.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp


sealed class BottomNavItem(val title: String, val icon: ImageVector) {
    object Home : BottomNavItem("Home", Icons.Default.Home)
    object Map : BottomNavItem("Map", Icons.Default.LocationOn)
    object Families : BottomNavItem("Families", Icons.Default.Group)
}
@Composable
fun NavBar(
    selectedItem: BottomNavItem,
    onItemSelected: (BottomNavItem) -> Unit
) {
    val items = listOf(BottomNavItem.Home, BottomNavItem.Map, BottomNavItem.Families)
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
                onClick = { onItemSelected(item) },

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
}