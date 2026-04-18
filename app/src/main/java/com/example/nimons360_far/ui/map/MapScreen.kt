package com.example.nimons360_far.ui.map

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

@OptIn(ExperimentalMaterial3Api::class)

data class UserLocationData(
    val userId: String?,
    val name: String?,
    val email: String?,
    val latitude: Double,
    val longitude: Double,
    val heading: Float,
    val battery: Int,
    val lastUpdateTime: Long = System.currentTimeMillis()
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
    val arrow_family = ContextCompat.getDrawable(context, R.drawable.arrow_family)
    val my_arrow = ContextCompat.getDrawable(context, R.drawable.my_arrow)
    
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
                    override fun longPressHelper(p: GeoPoint?): Boolean = false
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
                    myMarker.icon = my_arrow

                    val myData = UserLocationData(
                        userId = currentUser?.id?.toString(),
                        name = sessionManager.fetchUserName(),
                        email = sessionManager.fetchUserEmail(),
                        latitude = myLoc.latitude,
                        longitude = myLoc.longitude,
                        heading = currentHeading,
                        battery = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
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
                    familyMarker.icon = arrow_family

                    familyMarker.setOnMarkerClickListener { _, _ ->
                        selectedUser = member
                        showUserDialog = true
                        true
                    }
                    mapView.overlays.add(familyMarker)
                }

                mapView.invalidate()
            }
        )

        if (showUserDialog && selectedUser != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 24.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                UserInfoCard(user = selectedUser!!)
            }
        }
    }
}
