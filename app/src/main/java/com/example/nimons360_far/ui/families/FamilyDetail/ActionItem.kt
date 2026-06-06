package com.example.nimons360_far.ui.families

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nimons360_far.utils.AppColors.DesignTextBlack

// Reusable Composable - Action Item
@Composable
fun ActionItem(icon: String, iconBgColor: Color, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Wrapper
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconBgColor, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.width(16.dp))

        // Title Text
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            color = DesignTextBlack,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f)
        )

        // Chevron Arrow Right
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.LightGray.copy(alpha = 0.8f),
            modifier = Modifier.size(24.dp)
        )
    }
}
