package com.example.nimons360_far.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeadBar(currentTitle: String, onProfileClick: () -> Unit, selectedItem: BottomNavItem) {
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
            IconButton(onClick = onProfileClick) {
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = "Profile",
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    )
}