package com.example.nimons360_far.ui.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp

import android.annotation.SuppressLint
import com.example.nimons360_far.backend.models.PresencePayload
import com.example.nimons360_far.backend.network.MapWebSocketManager
import com.example.nimons360_far.backend.utils.SessionManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableFloatStateOf

import org.osmdroid.config.Configuration
import com.google.android.gms.location.LocationServices
import org.osmdroid.views.overlay.Marker
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

import com.example.nimons360_far.ui.components.UserInfoCard

@OptIn(ExperimentalMaterial3Api::class)

data class UserLocationData(
    val userId: String,
    val name: String,
    val email: String,
    val latitude: Double,
    val longitude: Double,
    val heading: Float,
    val battery: Int,
    val lastUpdateTime: Long = System.currentTimeMillis()
)

@SuppressLint("MissingPermission")
@Composable
fun MapScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var userGeoPoint by remember { mutableStateOf<GeoPoint?>(null) }

    var isMapCentered by remember { mutableStateOf(false) }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> hasLocationPermission = isGranted }
    )

    var selectedUser by remember { mutableStateOf<UserLocationData?>(null) }
    var showUserDialog by remember { mutableStateOf(false) }
//    TODO : TEST WEBSOCKET CONNECTION
    var familyMembers by remember { mutableStateOf(mapOf<Int, UserLocationData>()) }
    val sessionManager = remember { SessionManager(context) }
    val token = sessionManager.fetchAuthToken() ?: ""
    val wsManager = remember { MapWebSocketManager(token) }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    userGeoPoint = GeoPoint(location.latitude, location.longitude)
                }
            }
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

//    Get Family members data from websocket
    LaunchedEffect(Unit) {
        wsManager.connect()
        wsManager.onMemberUpdate = { payload ->
            val updatedList = familyMembers.toMutableMap()
            updatedList[payload.userId] = UserLocationData(
                userId = payload.userId.toString(),
                name = payload.fullName,
                email = payload.email,
                latitude = payload.latitude,
                longitude = payload.longitude,
                heading = payload.rotation,
                battery = payload.batteryLevel,
                lastUpdateTime = System.currentTimeMillis()
            )
            familyMembers = updatedList
        }
    }

//    Send User Data to WebSocket
    val userName = sessionManager.fetchUserName()
    var currentHeading by remember { mutableFloatStateOf(0f) }
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        val sensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    val orientationAngles = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)

                    var azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                    if (azimuth < 0) azimuth += 360f
                    currentHeading = azimuth
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(sensorEventListener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        onDispose {
            sensorManager.unregisterListener(sensorEventListener)
        }
    }
    LaunchedEffect(userGeoPoint) {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        while(true) {
            // -- CEK STATUS INTERNET --
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            val isMobileData = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
            val currentInternetStatus = if (isMobileData) "mobile" else "wifi"

            val currentBatteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val isCurrentlyCharging = batteryManager.isCharging

            userGeoPoint?.let { myLoc ->
                wsManager.sendPresence(
                    PresencePayload(
                        name = userName,
                        latitude = myLoc.latitude,
                        longitude = myLoc.longitude,
                        rotation = currentHeading,
                        batteryLevel = currentBatteryLevel,
                        isCharging = isCurrentlyCharging,
                        internetStatus = currentInternetStatus
                    )
                )
            }

            val now = System.currentTimeMillis()
            familyMembers = familyMembers.filter { (_, member) ->
                now - member.lastUpdateTime < 5000
            }

            kotlinx.coroutines.delay(1000)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (hasLocationPermission) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(18.0)

                        controller.setCenter(GeoPoint(-6.8915, 107.6106))

                        setOnClickListener {
                            selectedUser = null
                            showUserDialog = false
                        }
                    }
                },
                update = { mapView ->
                    mapView.overlays.clear()

                    userGeoPoint?.let { myLoc ->
                        if (!isMapCentered) {
                            mapView.controller.animateTo(myLoc)
                            isMapCentered = true
                        }

                        val myMarker = Marker(mapView)
                        myMarker.position = myLoc
                        myMarker.title = "Lokasi Saya Saat Ini"
                        myMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                        mapView.overlays.add(myMarker)
                    }

                    familyMembers.values.forEach { member ->
                        val familyMarker = Marker(mapView)
                        familyMarker.position = GeoPoint(member.latitude, member.longitude)
                        familyMarker.title = member.name
                        familyMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                        familyMarker.rotation = member.heading

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
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Aplikasi membutuhkan akses GPS untuk melihat keluarga Anda.")
                Button(onClick = { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }) {
                    Text("Berikan Izin Lokasi")
                }
            }
        }

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
