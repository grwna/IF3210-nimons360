package com.example.nimons360_far.ui.map

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration as AndroidConfig
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.nimons360_far.R
import com.example.nimons360_far.backend.data.AppDatabase
import com.example.nimons360_far.backend.data.FavoriteLocation
import com.example.nimons360_far.backend.data.LocationDbHelper
import com.example.nimons360_far.backend.data.LocationPhoto
import com.example.nimons360_far.backend.utils.SessionManager
import com.example.nimons360_far.backend.utils.UserRepository
import com.example.nimons360_far.ui.components.AddEditLocationBottomSheet
import com.example.nimons360_far.ui.components.FavoriteInfoCard
import com.example.nimons360_far.ui.components.MapActionBottomSheet
import com.example.nimons360_far.ui.components.UserInfoCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.util.UUID

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
    var batteryLevel by remember {
        val intent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        mutableStateOf(if (level >= 0) (level * 100 / scale) else 0)
    }

    // Daftarkan receiver agar battery level update secara real-time
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                if (level >= 0) batteryLevel = level * 100 / scale
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(receiver, filter)
        onDispose { context.unregisterReceiver(receiver) }
    }

    val screenConfig = LocalConfiguration.current
    val isLandscape = screenConfig.orientation == AndroidConfig.ORIENTATION_LANDSCAPE

    var isMapCentered by remember { mutableStateOf(false) }
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

    var selectedUserKey by rememberSaveable { mutableStateOf<String?>(null) }
    var showUserDialog by rememberSaveable { mutableStateOf(false) }
    var selectedUser by remember { mutableStateOf<UserLocationData?>(null) }

    var longPressLat by rememberSaveable { mutableStateOf<Double?>(null) }
    var longPressLon by rememberSaveable { mutableStateOf<Double?>(null) }
    val longPressGeoPoint = remember(longPressLat, longPressLon) {
        if (longPressLat != null && longPressLon != null) GeoPoint(longPressLat!!, longPressLon!!) else null
    }

    var showMapActionSheet by rememberSaveable { mutableStateOf(false) }
    var showAddEditSheet by rememberSaveable { mutableStateOf(false) }

    var editingFavoriteId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingPhotoPaths by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedFavoriteId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedFavoritePhotos by remember { mutableStateOf<List<LocationPhoto>>(emptyList()) }

    val selectedFavorite = favoriteLocations.firstOrNull { it.id == selectedFavoriteId }
    val editingFavorite = favoriteLocations.firstOrNull { it.id == editingFavoriteId }


    LaunchedEffect(Unit) { userRepository.fetchProfile() }

    // Load lokasi dari SQLite saat userId siap / berubah
    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                favoriteLocations = locationDbHelper.getFavoritesByUser(currentUserId)
            }
        }
    }

    LaunchedEffect(selectedUserKey, familyMembers, userGeoPoint) {
        when {
            selectedUserKey == null -> selectedUser = null
            selectedUserKey == "ME" && userGeoPoint != null -> {
                selectedUser = UserLocationData(
                    userId = currentUser?.id?.toString(),
                    name = sessionManager.fetchUserName(),
                    email = sessionManager.fetchUserEmail(),
                    latitude = userGeoPoint.latitude,
                    longitude = userGeoPoint.longitude,
                    heading = currentHeading,
                    battery = batteryLevel,
                    internet = internetStatus
                )
            }
            selectedUserKey != null && selectedUserKey != "ME" -> {
                val found = familyMembers.values.firstOrNull { it.userId == selectedUserKey }
                if (found != null) selectedUser = found
            }
        }
    }

    // Load foto favourite yang dipilih
    LaunchedEffect(selectedFavoriteId) {
        selectedFavoritePhotos = if (selectedFavoriteId != null) {
            withContext(Dispatchers.IO) {
                db.locationPhotoDao().getPhotosByLocationSync(selectedFavoriteId!!)
            }
        } else emptyList()
    }

    // Load path foto favourite yang sedang diedit
    LaunchedEffect(editingFavoriteId) {
        editingPhotoPaths = if (editingFavoriteId != null) {
            withContext(Dispatchers.IO) {
                db.locationPhotoDao().getPhotosByLocationSync(editingFavoriteId!!)
                    .map { it.filePath }
            }
        } else emptyList()
    }

    // Helper functions

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
                        selectedUserKey = null
                        showUserDialog = false
                        return true
                    }

                    override fun longPressHelper(p: GeoPoint?): Boolean {
                        if (p != null) {
                            longPressLat = p.latitude
                            longPressLon = p.longitude
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

                    myMarker.setOnMarkerClickListener { _, _ ->
                        selectedUserKey = "ME"
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
                        selectedUserKey = member.userId
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
                        selectedUserKey = null
                        selectedFavoriteId = fav.id   // foto diload via LaunchedEffect
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
                .padding(
                    end = 16.dp,
                    bottom = when {
                        showUserDialog && selectedUser != null -> if (isLandscape) 220.dp else 280.dp
                        else -> 24.dp
                    }
                ),
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
                modifier = Modifier
                    .fillMaxWidth(if (isLandscape) 0.55f else 1f)
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
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
                        selectedUserKey = null
                    },
                    onClose = {
                        showUserDialog = false
                        selectedUserKey = null
                    }
                )
            }
        }

        // Map Action Bottom Sheet (long press)
        if (showMapActionSheet && longPressGeoPoint != null) {
            MapActionBottomSheet(
                geoPoint = longPressGeoPoint!!,
                onDismiss = { showMapActionSheet = false },
                onMarkLocation = {
                    editingFavoriteId = null   // lokasi baru, bukan edit
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
                            editingFavoriteId = null
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
                onClose = { selectedFavoriteId = null },
                onEdit = {
                    editingFavoriteId = selectedFavoriteId   // foto diedit via LaunchedEffect
                    selectedFavoriteId = null
                    showAddEditSheet = true
                },
                onDelete = {
                    val toDeleteId = selectedFavoriteId
                    selectedFavoriteId = null
                    coroutineScope.launch(Dispatchers.IO) {
                        if (toDeleteId != null) {
                            deleteLocationPhotos(toDeleteId)
                            locationDbHelper.deleteFavorite(toDeleteId)
                        }
                        val updated = locationDbHelper.getFavoritesByUser(currentUserId)
                        withContext(Dispatchers.Main) { favoriteLocations = updated }
                    }
                }
            )
        }
    }
}
