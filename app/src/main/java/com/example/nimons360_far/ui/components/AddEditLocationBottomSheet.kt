package com.example.nimons360_far.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.nimons360_far.backend.data.FavoriteLocation
import com.example.nimons360_far.utils.AppColors
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditLocationBottomSheet(
    existingLocation: FavoriteLocation? = null,
    existingPhotoPaths: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (name: String, description: String, photosToAdd: List<Uri>, photosToRemove: List<String>) -> Unit
) {
    val context = LocalContext.current

    var locationName by remember { mutableStateOf(existingLocation?.name ?: "") }
    var description by remember { mutableStateOf(existingLocation?.description ?: "") }

    // Track existing and new photos
    val removedPhotoPaths = remember { mutableStateListOf<String>() }
    val newPhotoUris = remember { mutableStateListOf<Uri>() }

    // Camera temp file
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    // Permission state
    var showPermissionDialog by remember { mutableStateOf(false) }
    var permissionType by remember { mutableStateOf("") }

    // Launchers
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        newPhotoUris.addAll(uris)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraUri?.let { newPhotoUris.add(it) }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // Launch camera after permission granted
            val tempDir = File(context.cacheDir, "camera_temp").also { it.mkdirs() }
            val tempFile = File.createTempFile("cam_", ".jpg", tempDir)
            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", tempFile
            )
            cameraUri = uri
            cameraLauncher.launch(uri)
        }
    }

    val galleryPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) galleryLauncher.launch("image/*")
    }

    fun launchGallery() {
        val permission = if (android.os.Build.VERSION.SDK_INT >= 33)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            galleryLauncher.launch("image/*")
        } else {
            galleryPermissionLauncher.launch(permission)
        }
    }

    fun launchCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val tempDir = File(context.cacheDir, "camera_temp").also { it.mkdirs() }
            val tempFile = File.createTempFile("cam_", ".jpg", tempDir)
            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", tempFile
            )
            cameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val visibleExistingPhotos = existingPhotoPaths.filter { it !in removedPhotoPaths }
    val isFormValid = locationName.isNotBlank()

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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (existingLocation != null) "Edit Lokasi" else "Tandai Lokasi",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1C1E)
            )

            // Name Field
            OutlinedTextField(
                value = locationName,
                onValueChange = { locationName = it },
                label = { Text("Nama Lokasi *", color = Color(0xFF8E8E93)) },
                placeholder = { Text("Misal: Rumah, Kantor...", color = Color(0xFFAAAAAA)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF007AFF),
                    unfocusedBorderColor = Color(0xFFD1D1D6),
                    focusedTextColor = Color(0xFF1C1C1E),
                    unfocusedTextColor = Color(0xFF1C1C1E),
                    cursorColor = Color(0xFF007AFF),
                    focusedLabelColor = Color(0xFF007AFF),
                    unfocusedLabelColor = Color(0xFF8E8E93)
                )
            )

            // Description Field
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Deskripsi", color = Color(0xFF8E8E93)) },
                placeholder = { Text("Tambahkan catatan tentang lokasi ini...", color = Color(0xFFAAAAAA)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF007AFF),
                    unfocusedBorderColor = Color(0xFFD1D1D6),
                    focusedTextColor = Color(0xFF1C1C1E),
                    unfocusedTextColor = Color(0xFF1C1C1E),
                    cursorColor = Color(0xFF007AFF),
                    focusedLabelColor = Color(0xFF007AFF),
                    unfocusedLabelColor = Color(0xFF8E8E93)
                )
            )

            Text(
                text = "Foto Lokasi",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF3C3C43)
            )

            // Photo list
            val totalPhotos = visibleExistingPhotos.size + newPhotoUris.size
            if (totalPhotos > 0) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Existing photos
                    items(visibleExistingPhotos) { path ->
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = File(path),
                                contentDescription = "Foto Lokasi",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            // Remove button
                            IconButton(
                                onClick = { removedPhotoPaths.add(path) },
                                modifier = Modifier
                                    .size(24.dp)
                                    .align(Alignment.TopEnd)
                                    .background(Color(0xBB000000), CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Hapus",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                    // New photos
                    items(newPhotoUris.toList()) { uri ->
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = uri,
                                contentDescription = "Foto Baru",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { newPhotoUris.remove(uri) },
                                modifier = Modifier
                                    .size(24.dp)
                                    .align(Alignment.TopEnd)
                                    .background(Color(0xBB000000), CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Hapus",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Add photo buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { launchGallery() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFD1D1D6)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF3C3C43)
                    )
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Galeri", fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = { launchCamera() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFD1D1D6)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF3C3C43)
                    )
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Kamera", fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFD1D1D6)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF3C3C43))
                ) {
                    Text("Batal", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        onSave(
                            locationName.trim(),
                            description.trim(),
                            newPhotoUris.toList(),
                            removedPhotoPaths.toList()
                        )
                    },
                    enabled = isFormValid,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF007AFF),
                        disabledContainerColor = Color(0xFFB0C8F0)
                    )
                ) {
                    Text("Simpan", fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
    }
}
