package com.example.nimons360_far.ui.map

import android.content.Context
import android.net.Uri
import android.view.MotionEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import android.annotation.SuppressLint
import android.net.NetworkCapabilities
import android.net.ConnectivityManager
import com.example.nimons360_far.backend.utils.SessionManager
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import com.example.nimons360_far.backend.utils.UserRepository

import org.osmdroid.config.Configuration
import org.osmdroid.views.overlay.Marker
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.overlay.MapEventsOverlay
import com.example.nimons360_far.R
import com.example.nimons360_far.ui.components.UserInfoCard
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import com.example.nimons360_far.backend.data.AppDatabase
import com.example.nimons360_far.backend.data.FavoriteLocation
import com.example.nimons360_far.backend.data.LocationDbHelper
import com.example.nimons360_far.backend.data.LocationPhoto
import com.example.nimons360_far.ui.components.FavoriteInfoCard
import com.example.nimons360_far.ui.components.MapActionBottomSheet
import com.example.nimons360_far.ui.components.AddEditLocationBottomSheet
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)

data class UserLocationData(
    val userId: String?,
    val name: String?,
    val email: String?,
    val latitude: Double,
    val longitude: Double,
    val heading: Float,
    val battery: Int,
    val lastUpdateTime: Long = System.currentTimeMillis(),
    val internet: String
)

@SuppressLint("MissingPermission")
@Composable
fun MapScreen(
    userGeoPoint: GeoPoint?,
    currentHeading: Float,
    familyMembers: Map<Int, UserLocationData>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
    val batteryManager = remember { context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager }

    var isMapCentered by remember { mutableStateOf(false) }
    var selectedUser by remember { mutableStateOf<UserLocationData?>(null) }
    var showUserDialog by remember { mutableStateOf(false) }
    val userRepository = remember { UserRepository.getInstance(context) }
    val currentUser by userRepository.userProfile.collectAsState()
    val sessionManager = remember { SessionManager(context) }
    val arrowFamily = ContextCompat.getDrawable(context, R.drawable.arrow_family)
    val myArrow = ContextCompat.getDrawable(context, R.drawable.my_arrow)

    val internetStatus = remember(userGeoPoint) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        val isMobileData = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        if (isMobileData) "mobile" else "wifi"
    }

    // Room database (untuk LocationPhoto & PinnedFamily)
    val db = remember { AppDatabase.getDatabase(context) }
    // SQLite helper (untuk FavoriteLocation)
    val locationDbHelper = remember { LocationDbHelper.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()
    val currentUserId = currentUser?.id?.toString() ?: ""

    // State lokasi favorit, direfresh manual tiap ada perubahan
    var favoriteLocations by remember { mutableStateOf<List<FavoriteLocation>>(emptyList()) }
    val favIcon = ContextCompat.getDrawable(context, R.drawable.ic_fav_location)

    val isMultiTouch = remember { booleanArrayOf(false) }

    val mapViewRef = remember { mutableStateOf<MapView?>(null) }

    // Bottom sheet states
    var longPressGeoPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var showMapActionSheet by remember { mutableStateOf(false) }
    var showAddEditSheet by remember { mutableStateOf(false) }
    var editingFavorite by remember { mutableStateOf<FavoriteLocation?>(null) }
    var editingPhotoPaths by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedFavorite by remember { mutableStateOf<FavoriteLocation?>(null) }
    var selectedFavoritePhotos by remember { mutableStateOf<List<LocationPhoto>>(emptyList()) }

    // Fetch profile to get id
    LaunchedEffect(Unit) {
        userRepository.fetchProfile()
    }

    // Load lokasi dari SQLite saat userId siap / berubah
    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                favoriteLocations = locationDbHelper.getFavoritesByUser(currentUserId)
            }
        }
    }

    // Helper untuk refresh state setelah write
    fun refreshFavorites() {
        coroutineScope.launch(Dispatchers.IO) {
            val updated = locationDbHelper.getFavoritesByUser(currentUserId)
            withContext(Dispatchers.Main) { favoriteLocations = updated }
        }
    }

    // Helper untuk copy Uri to internal storage, returns file path
    fun copyPhotoToStorage(uri: Uri): String? {
        return try {
            val dir = File(context.filesDir, "location_photos").also { it.mkdirs() }
            val fileName = "loc_${UUID.randomUUID()}.jpg"
            val destFile = File(dir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            }
            destFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    // Helper untuk delete semua foto untuk lokasi dari filesystem
    suspend fun deleteLocationPhotos(locationId: String) {
        val photos = db.locationPhotoDao().getPhotosByLocationSync(locationId)
        photos.forEach { photo ->
            try { File(photo.filePath).delete() } catch (_: Exception) {}
        }
        db.locationPhotoDao().deletePhotosByLocation(locationId)
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
                    controller.setZoom(18.0)
                    controller.setCenter(GeoPoint(-6.8915, 107.6106))
                }
            },
            update = { mapView ->
                mapViewRef.value = mapView
                mapView.overlays.clear()

                val mapEventsReceiver = object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                        selectedUser = null
                        showUserDialog = false
                        return true
                    }

                    override fun longPressHelper(p: GeoPoint?): Boolean {
                        if (p != null) {
                            longPressGeoPoint = p
                            showMapActionSheet = true
                        }
                        return true
                    }
                }
                mapView.overlays.add(MapEventsOverlay(mapEventsReceiver))

                userGeoPoint?.let { myLoc ->
                    if (!isMapCentered) {
                        mapView.controller.animateTo(myLoc)
                        isMapCentered = true
                    }

                    val myMarker = Marker(mapView)
                    myMarker.position = myLoc
                    myMarker.title = "Lokasi Saya Saat Ini"
                    myMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    myMarker.rotation = currentHeading
                    myMarker.icon = myArrow

                    val myData = UserLocationData(
                        userId = currentUser?.id?.toString(),
                        name = sessionManager.fetchUserName(),
                        email = sessionManager.fetchUserEmail(),
                        latitude = myLoc.latitude,
                        longitude = myLoc.longitude,
                        heading = currentHeading,
                        battery = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY),
                        internet = internetStatus
                    )

                    myMarker.setOnMarkerClickListener { _, _ ->
                        selectedUser = myData
                        showUserDialog = true
                        true
                    }
                    mapView.overlays.add(myMarker)
                }

                familyMembers.values.forEach { member ->
                    val familyMarker = Marker(mapView)
                    familyMarker.position = GeoPoint(member.latitude, member.longitude)
                    familyMarker.title = member.name
                    familyMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_TOP)
                    familyMarker.rotation = member.heading
                    familyMarker.icon = arrowFamily

                    familyMarker.setOnMarkerClickListener { _, _ ->
                        selectedUser = member
                        showUserDialog = true
                        true
                    }
                    mapView.overlays.add(familyMarker)
                }

                favoriteLocations.forEach { fav ->
                    val favMarker = Marker(mapView)
                    favMarker.position = GeoPoint(fav.latitude, fav.longitude)
                    favMarker.icon = favIcon
                    favMarker.title = fav.name

                    favMarker.setOnMarkerClickListener { _, _ ->
                        showUserDialog = false
                        selectedUser = null
                        coroutineScope.launch {
                            val photos = withContext(Dispatchers.IO) {
                                db.locationPhotoDao().getPhotosByLocationSync(fav.id)
                            }
                            selectedFavoritePhotos = photos
                            selectedFavorite = fav
                        }
                        true
                    }
                    mapView.overlays.add(favMarker)
                }

                mapView.invalidate()
            }
        )

        // Tombol recenter
        FloatingActionButton(
            onClick = {
                userGeoPoint?.let { loc -> mapViewRef.value?.controller?.animateTo(loc) }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 24.dp),
            shape = CircleShape,
            containerColor = Color.White,
            contentColor = Color(0xFF1976D2)
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Recenter ke lokasi saya"
            )
        }

        // User info card
        if (showUserDialog && selectedUser != null) {
            Box(
                modifier = Modifier .fillMaxSize() .padding(bottom = 24.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                UserInfoCard(
                    user = selectedUser!!,
                    onSendGreeting = { message ->
                        selectedUser?.userId?.toIntOrNull()?.let { targetId ->
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val api = com.example.nimons360_far.backend.network.ApiClient.getNotificationApi(context)
                                    val familyApi = com.example.nimons360_far.backend.network.ApiClient.getFamilyApi(context)
                                    val familiesRes = familyApi.getMyFamilies()
                                    val family = familiesRes.body()?.data?.firstOrNull()
                                    val familyId = family?.id ?: 1
                                    val familyName = family?.name ?: "Family"
                                    val prependedMessage = "$familyName: $message"
                                    
                                    val req = com.example.nimons360_far.backend.models.SendGreetingRequest(familyId, targetId, prependedMessage)
                                    api.sendGreeting(req)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                        showUserDialog = false
                    },
                    onClose = { showUserDialog = false }
                )
            }
        }

        // Map Action Bottom Sheet (long press)
        if (showMapActionSheet && longPressGeoPoint != null) {
            MapActionBottomSheet(
                geoPoint = longPressGeoPoint!!,
                onDismiss = { showMapActionSheet = false },
                onMarkLocation = {
                    editingFavorite = null
                    editingPhotoPaths = emptyList()
                    showAddEditSheet = true
                }
            )
        }

        // Add / Edit Location Bottom Sheet
        if (showAddEditSheet) {
            AddEditLocationBottomSheet(
                existingLocation = editingFavorite,
                existingPhotoPaths = editingPhotoPaths,
                onDismiss = { showAddEditSheet = false },
                onSave = { name, description, newUris, removedPaths ->
                    coroutineScope.launch(Dispatchers.IO) {
                        val locationId = editingFavorite?.id ?: UUID.randomUUID().toString()

                        // Simpan atau perbarui lokasi di SQLite
                        val location = FavoriteLocation(
                            id = locationId,
                            userId = currentUserId,
                            name = name,
                            description = description,
                            latitude = editingFavorite?.latitude ?: longPressGeoPoint!!.latitude,
                            longitude = editingFavorite?.longitude ?: longPressGeoPoint!!.longitude
                        )
                        if (editingFavorite != null) {
                            locationDbHelper.updateFavorite(location)
                        } else {
                            locationDbHelper.insertFavorite(location)
                        }

                        // Remove photos that were deleted by user
                        removedPaths.forEach { path ->
                            try { File(path).delete() } catch (_: Exception) {}
                            val allPhotos = db.locationPhotoDao().getPhotosByLocationSync(locationId)
                            allPhotos.filter { it.filePath == path }.forEach { photo ->
                                db.locationPhotoDao().deletePhoto(photo.id)
                            }
                        }

                        // Copy and save new photos
                        newUris.forEach { uri ->
                            val filePath = copyPhotoToStorage(uri)
                            if (filePath != null) {
                                val photo = LocationPhoto(
                                    id = UUID.randomUUID().toString(),
                                    locationId = locationId,
                                    filePath = filePath
                                )
                                db.locationPhotoDao().insertPhoto(photo)
                            }
                        }

                        // Refresh UI + reset state setelah semua pekerjaan selesai
                        val updated = locationDbHelper.getFavoritesByUser(currentUserId)
                        withContext(Dispatchers.Main) {
                            favoriteLocations = updated
                            showAddEditSheet = false
                            editingFavorite = null
                        }
                    }
                }
            )
        }

        // Favorite location detail bottom sheet
        if (selectedFavorite != null) {
            FavoriteInfoCard(
                favorite = selectedFavorite!!,
                photos = selectedFavoritePhotos,
                onClose = { selectedFavorite = null },
                onEdit = {
                    editingFavorite = selectedFavorite
                    editingPhotoPaths = selectedFavoritePhotos.map { it.filePath }
                    selectedFavorite = null
                    showAddEditSheet = true
                },
                onDelete = {
                    val toDelete = selectedFavorite!!
                    selectedFavorite = null
                    coroutineScope.launch(Dispatchers.IO) {
                        deleteLocationPhotos(toDelete.id)
                        locationDbHelper.deleteFavorite(toDelete.id)
                        val updated = locationDbHelper.getFavoritesByUser(currentUserId)
                        withContext(Dispatchers.Main) { favoriteLocations = updated }
                    }
                }
            )
        }
    }
}
