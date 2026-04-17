package com.example.nimons360_far.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.graphics.Color


@Composable
fun FloatingFamilies(
    onClick: () -> Unit
) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = Color(0xFFE3EEFF),
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = "Add",
            tint = Color(0xFF1565C0)
        )
    }
}