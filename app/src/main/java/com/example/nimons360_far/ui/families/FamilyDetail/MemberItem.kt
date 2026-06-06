package com.example.nimons360_far.ui.families

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.CircularProgressIndicator
import coil.compose.SubcomposeAsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nimons360_far.utils.AppColors.DesignBlue
import com.example.nimons360_far.utils.AppColors.DesignTextBlack
import com.example.nimons360_far.utils.AppColors.DesignTextGrey
import com.example.nimons360_far.utils.AppColors.RedDanger

// Reusable Composable - Member Item
@Composable
fun MemberItem(name: String, email: String, isJoined: Boolean, isMe: Boolean, profileImageUrl: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Get Profile Image URL
        val finalImageUrl = if (profileImageUrl?.startsWith("/") == true) {
            "https://mad.labpro.hmif.dev$profileImageUrl"
        } else {
            profileImageUrl
        }

        if (isJoined) {
            // Show Member Detail
            SubcomposeAsyncImage(
                model = finalImageUrl,
                contentDescription = "Profile Photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(40.dp).clip(CircleShape),
                loading = { CircularProgressIndicator(modifier = Modifier.padding(8.dp), color = DesignBlue, strokeWidth = 2.dp) },
                error = {
                    Box(modifier = Modifier.fillMaxSize().background(DesignBlue), contentAlignment = Alignment.Center) {
                        Text(name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            )
            Spacer(modifier = Modifier.width(16.dp))

            // Username & Email
            Column(modifier = Modifier.weight(1f)) {
                // Username
                Text(name, fontWeight = FontWeight.Bold, color = DesignTextBlack, fontSize = 15.sp)

                // Email
                Text(email, color = DesignTextGrey, fontSize = 12.sp)
            }

            // You Tag
            if (isMe) {
                Surface(color = YellowWarningBg, shape = RoundedCornerShape(12.dp)) {
                    Text(
                        "You",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        color = YellowWarningBorder,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            // Hide Member Detail
            val avatarColors = listOf(DesignBlue, RedDanger, Color(0xFF388E3C), Color(0xFFFBC02D), Color(0xFF7B1FA2))
            val safeHash = name.hashCode().let { if (it < 0) it * -1 else it }
            val blurColor = avatarColors[safeHash % avatarColors.size]

            // User Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .blur(radius = 12.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    .background(blurColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.6f))
            }
            Spacer(modifier = Modifier.width(16.dp))

            // Username & Email
            Column(modifier = Modifier.weight(1f)) {
                // Username
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .height(14.dp)
                        .background(Color.LightGray.copy(alpha = 0.8f), RoundedCornerShape(7.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Email
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(12.dp)
                        .background(Color.LightGray.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                )
            }
        }
    }
}
