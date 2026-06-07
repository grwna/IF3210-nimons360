package com.example.nimons360_far.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.os.BatteryManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.nimons360_far.R
import com.example.nimons360_far.backend.models.PresencePayload
import com.example.nimons360_far.backend.network.MapWebSocketManager
import com.example.nimons360_far.backend.utils.SessionManager
import com.example.nimons360_far.ui.map.UserLocationData
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.osmdroid.util.GeoPoint

class LocationTrackingService : Service(), SensorEventListener {

    inner class LocalBinder : Binder() {
        fun getService(): LocationTrackingService = this@LocationTrackingService
    }

    private val binder = LocalBinder()

    // State flows, dibaca oleh MainActivity via Binder
    private val _userGeoPoint = MutableStateFlow<GeoPoint?>(null)
    val userGeoPoint: StateFlow<GeoPoint?> = _userGeoPoint

    private val _currentHeading = MutableStateFlow(0f)
    val currentHeading: StateFlow<Float> = _currentHeading

    private val _familyMembers = MutableStateFlow<Map<Int, UserLocationData>>(emptyMap())
    val familyMembers: StateFlow<Map<Int, UserLocationData>> = _familyMembers

    // Internal state 
    private var batteryLevel = 0
    private var isCharging = false
    private var locationUpdatesStarted = false

    private lateinit var wsManager: MapWebSocketManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var sensorManager: SensorManager
    private lateinit var sessionManager: SessionManager
    private var rotationSensor: Sensor? = null

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var presenceJob: Job? = null

    // BroadcastReceiver battery
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            batteryLevel = if (scale > 0) (level * 100f / scale).toInt() else 0

            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
                    || status == BatteryManager.BATTERY_STATUS_FULL
        }
    }

    // Ketika GPS dimatikan lalu dihidupkan, restart location updates otomatis
    // Ketika permission dimatikan lalu dihidupkan, tidak ada aksi, user harus buka app lagi
    private val gpsProviderReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != LocationManager.PROVIDERS_CHANGED_ACTION) return
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (isGpsEnabled) {
                // Hanya restart jika permission masih ada, tidak auto-start jika permission dicabut
                val hasPermission = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                if (hasPermission) {
                    startLocationUpdates()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        sessionManager = SessionManager(this)

        val token = sessionManager.fetchAuthToken() ?: ""
        wsManager = MapWebSocketManager(token)
        wsManager.connect()
        wsManager.onMemberUpdate = { payload ->
            val updated = _familyMembers.value.toMutableMap()
            updated[payload.userId] = UserLocationData(
                userId = payload.userId.toString(),
                name = payload.fullName,
                email = payload.email,
                latitude = payload.latitude,
                longitude = payload.longitude,
                heading = payload.rotation,
                battery = payload.batteryLevel,
                lastUpdateTime = System.currentTimeMillis(),
                internet = payload.internetStatus
            )
            _familyMembers.value = updated
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    _userGeoPoint.value = GeoPoint(loc.latitude, loc.longitude)
                }
            }
        }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), Context.RECEIVER_NOT_EXPORTED)
            registerReceiver(gpsProviderReceiver, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION), Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            registerReceiver(gpsProviderReceiver, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))
        }

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startLocationUpdates()
        }

        startPresenceLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val restartIntent = Intent(applicationContext, LocationTrackingService::class.java)
        val pendingIntent = PendingIntent.getService(
            applicationContext,
            RESTART_REQUEST_CODE,
            restartIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.set(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + 1_000L,
            pendingIntent
        )
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        presenceJob?.cancel()
        serviceScope.cancel()
        if (locationUpdatesStarted) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
        sensorManager.unregisterListener(this)
        try { unregisterReceiver(batteryReceiver) } catch (_: Exception) {}
        try { unregisterReceiver(gpsProviderReceiver) } catch (_: Exception) {}
        wsManager.disconnect()
    }


    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (locationUpdatesStarted) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2_000L).build()
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        locationUpdatesStarted = true
    }

    private fun startPresenceLoop() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val userName = sessionManager.fetchUserName() ?: "User"

        presenceJob = serviceScope.launch {
            while (isActive) {
                try {
                    val network = connectivityManager.activeNetwork
                    val capabilities = connectivityManager.getNetworkCapabilities(network)
                    val isMobile = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
                    val internetStatus = if (isMobile) "mobile" else "wifi"

                    _userGeoPoint.value?.let { loc ->
                        wsManager.sendPresence(
                            PresencePayload(
                                name = userName,
                                latitude = loc.latitude,
                                longitude = loc.longitude,
                                rotation = _currentHeading.value,
                                batteryLevel = batteryLevel,
                                isCharging = isCharging,
                                internetStatus = internetStatus
                            )
                        )
                    }

                    val now = System.currentTimeMillis()
                    _familyMembers.value = _familyMembers.value.filter { (_, member) ->
                        now - member.lastUpdateTime < 5_000L
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    try { wsManager.connect() } catch (_: Exception) {}
                }

                delay(1_000L)
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ROTATION_VECTOR) return
        val rotationMatrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        val orientationAngles = FloatArray(3)
        SensorManager.getOrientation(rotationMatrix, orientationAngles)
        var azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
        azimuth = -azimuth
        if (azimuth < 0) azimuth += 360f
        _currentHeading.value = azimuth
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}


    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Location Tracking",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Mengirim lokasi Anda ke anggota keluarga"
            setShowBadge(false)
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openAppIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingOpenApp = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Nimons360 aktif")
            .setContentText("Lokasi Anda sedang dibagikan ke anggota keluarga")
            .setSmallIcon(R.drawable.my_arrow)
            .setContentIntent(pendingOpenApp)
            .setOngoing(true)          // tidak bisa di-swipe
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "nimons_location_tracking"
        const val NOTIFICATION_ID = 1001
        private const val RESTART_REQUEST_CODE = 9001

        fun start(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java)
            context.stopService(intent)
        }
    }
}
