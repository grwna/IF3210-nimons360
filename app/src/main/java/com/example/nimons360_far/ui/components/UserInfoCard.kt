package com.example.nimons360_far.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nimons360_far.ui.map.UserLocationData

@Composable
fun UserInfoCard(
    user: UserLocationData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(366.dp)
            .height(172.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color(0x26000000),
                ambientColor = Color(0x1A000000)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFFBFE)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2196F3)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.name.firstOrNull()?.uppercase() ?: "U",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = user.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF1C1B1F)
                    )
                    user.email?.let {
                        Text(
                            text = it,
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoMiniCard(
                    icon = Icons.Default.BatteryFull,
                    iconTint = Color(0xFF4CAF50), // Hijau
                    value = "${user.battery}%",
                    label = "Battery",
                    modifier = Modifier.weight(1f)
                )

                InfoMiniCard(
                    icon = Icons.Default.LocationOn,
                    iconTint = Color(0xFFF44336), // Merah
                    value = "${"%.3f".format(user.latitude)}, ${"%.3f".format(user.longitude)}",
                    label = "Location",
                    modifier = Modifier.weight(1f)
                )

                InfoMiniCard(
                    icon = Icons.Default.Language,
                    iconTint = Color(0xFF64B5F6), // Biru muda
                    value = "Wi-Fi",
                    label = "Network",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun InfoMiniCard(
    icon: ImageVector,
    iconTint: Color,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF4F5F7)) // Warna abu-abu sangat muda
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
            Text(text = value, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(text = label, color = Color.Gray, fontSize = 10.sp)
        }
    }
}