package com.example.nimons360_far.ui.map

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
import com.example.nimons360_far.backend.data.AppDatabase
import com.example.nimons360_far.backend.data.FavoriteLocation
import com.example.nimons360_far.ui.components.FavoriteInfoCard

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

//    Favorite Location stuff
    val db = remember { AppDatabase.getDatabase(context) }
    val coroutineScope = rememberCoroutineScope()
    val currentUserId = currentUser?.id?.toString() ?: ""
    val favoriteLocations by db.favoriteLocationDao().getFavoritesByUser(currentUserId).collectAsState(initial = emptyList())
    var showFavDialog by remember { mutableStateOf(false) }
    var tempFavGeoPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var favLocationName by remember { mutableStateOf("") }
    val favIcon = ContextCompat.getDrawable(context, R.drawable.ic_fav_location)
    var selectedFavorite by remember { mutableStateOf<FavoriteLocation?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var newLocationName by remember { mutableStateOf("") }

    // Fetch profile to get id
    LaunchedEffect(Unit) {
        userRepository.fetchProfile()
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(18.0)
                    controller.setCenter(GeoPoint(-6.8915, 107.6106))
                }
            },
            update = { mapView ->
                mapView.overlays.clear()

                val mapEventsReceiver = object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                        selectedUser = null
                        showUserDialog = false
                        return true
                    }

                    override fun longPressHelper(p: GeoPoint?): Boolean {
                        if (p != null) {
                            tempFavGeoPoint = p
                            favLocationName = ""
                            showFavDialog = true
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

                    favMarker.setOnMarkerClickListener { _, _ ->
                        showUserDialog = false
                        selectedFavorite = fav
                        true
                    }
                    mapView.overlays.add(favMarker)
                }

                mapView.invalidate()
            }
        )

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
        if (showFavDialog) {
            AlertDialog(
                onDismissRequest = { showFavDialog = false },
                title = { Text("Simpan Lokasi Favorit") },
                text = {
                    Column {
                        Text("Masukkan nama lokasi:")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = favLocationName,
                            onValueChange = { favLocationName = it },
                            placeholder = { Text("Misal: Rumah") },
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    val isFormValid = favLocationName.isNotBlank() && tempFavGeoPoint != null && currentUserId.isNotEmpty()
                    Button(
                        onClick = {
                            if (favLocationName.isNotBlank() && tempFavGeoPoint != null && currentUserId.isNotEmpty()) {
                                coroutineScope.launch(Dispatchers.IO) {
                                    val newFav = FavoriteLocation(
                                        id = UUID.randomUUID().toString(),
                                        userId = currentUserId,
                                        name = favLocationName,
                                        latitude = tempFavGeoPoint!!.latitude,
                                        longitude = tempFavGeoPoint!!.longitude
                                    )
                                    db.favoriteLocationDao().insertFavorite(newFav)
                                }
                                showFavDialog = false
                            }
                        },
                        enabled = isFormValid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3)
                        )
                    ) {
                        Text("Simpan")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showFavDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD54848)
                        )
                    ) {
                        Text("Batal")
                    }
                }
            )
        }
        if (selectedFavorite != null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(bottom = 24.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                FavoriteInfoCard(
                    favorite = selectedFavorite!!,
                    onClose = { selectedFavorite = null },
                    onRename = {
                        newLocationName = selectedFavorite!!.name
                        showRenameDialog = true
                    },
                    onDelete = {
                        coroutineScope.launch(Dispatchers.IO) {
                            db.favoriteLocationDao().deleteFavorite(selectedFavorite!!.id)
                            selectedFavorite = null
                        }
                    }
                )
            }
        }

        if (showRenameDialog) {
            AlertDialog(
                onDismissRequest = { showRenameDialog = false },
                title = { Text("Ubah Nama Lokasi") },
                text = {
                    OutlinedTextField(
                        value = newLocationName,
                        onValueChange = { newLocationName = it },
                        label = { Text("Nama Baru") }
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        selectedFavorite?.let { fav ->
                            coroutineScope.launch(Dispatchers.IO) {
                                val updatedFav = fav.copy(name = newLocationName)
                                db.favoriteLocationDao().insertFavorite(updatedFav)
                                selectedFavorite = null
                                showRenameDialog = false
                            }
                        }
                    }) { Text("Simpan") }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameDialog = false }) { Text("Batal") }
                }
            )
        }
    }
}
