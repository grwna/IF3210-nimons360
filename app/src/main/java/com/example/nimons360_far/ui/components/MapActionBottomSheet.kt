package com.example.nimons360_far.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nimons360_far.utils.AppColors
import org.osmdroid.util.GeoPoint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapActionBottomSheet(
    geoPoint: GeoPoint,
    onDismiss: () -> Unit,
    onMarkLocation: () -> Unit
) {
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        contentColor = Color(0xFF1C1C1E),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Pilih Aksi",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1C1E),
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Text(
                text = "Lat: ${"%.5f".format(geoPoint.latitude)}, Lon: ${"%.5f".format(geoPoint.longitude)}",
                fontSize = 12.sp,
                color = Color(0xFF8E8E93),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Tandai Lokasi Button
            Button(
                onClick = {
                    onDismiss()
                    onMarkLocation()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.BlueAction
                )
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tandai Lokasi",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Buka Google Maps Button
            OutlinedButton(
                onClick = {
                    val uri = Uri.parse("geo:${geoPoint.latitude},${geoPoint.longitude}?q=${geoPoint.latitude},${geoPoint.longitude}")
                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                        setPackage("com.google.android.apps.maps")
                    }
                    // Fallback jika Google Maps blum diinstal
                    val chooserIntent = Intent.createChooser(
                        Intent(Intent.ACTION_VIEW, uri), "Buka dengan"
                    )
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        context.startActivity(chooserIntent)
                    }
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF4CAF50)
                ),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF4CAF50))
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Buka di Google Maps",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
