package com.example.nimons360_far.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nimons360_far.ui.map.UserLocationData
import java.util.Calendar

@Composable
fun SendGreetingSection(
    user: UserLocationData,
    modifier: Modifier = Modifier,
    onSendGreeting: ((String) -> Unit)? = null
) {
    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val (greeting, emoji, timeLabel) = when (currentHour) {
        in 5..11 -> Triple("Good Morning!", "🌅", "MORNING")
        in 12..17 -> Triple("Good Afternoon!", "🌇", "AFTERNOON")
        else -> Triple("Good Night!", "🌆", "NIGHT")
    }

    val timeFormat = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
    val currentTimeStr = timeFormat.format(java.util.Date())

    Column(modifier = modifier.fillMaxWidth()) {
        Text("SEND A GREETING", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = com.example.nimons360_far.utils.AppColors.DesignTextGrey)
        Spacer(modifier = Modifier.height(12.dp))
        
        // Dynamic Card Based on Time of Day
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(com.example.nimons360_far.utils.AppColors.LightBlueBg)
                .border(1.dp, com.example.nimons360_far.utils.AppColors.DesignBlue.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Emoji Box
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji, fontSize = 32.sp)
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = "$timeLabel · $currentTimeStr",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = com.example.nimons360_far.utils.AppColors.DesignTextGrey
                    )
                    Text(
                        text = greeting,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = com.example.nimons360_far.utils.AppColors.DesignBlue
                    )
                    Text(
                        text = "Auto-selected based on current time",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Primary Action
        androidx.compose.material3.Button(
            onClick = { onSendGreeting?.invoke(greeting) },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = com.example.nimons360_far.utils.AppColors.DesignBlue
            )
        ) {
            Text("Send $greeting $emoji", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
