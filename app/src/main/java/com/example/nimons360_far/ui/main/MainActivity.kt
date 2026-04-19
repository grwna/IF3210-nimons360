package com.example.nimons360_far.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Bundle
import android.os.Looper
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.nimons360_far.R
import com.example.nimons360_far.backend.models.PresencePayload
import com.example.nimons360_far.backend.network.MapWebSocketManager
import com.example.nimons360_far.backend.utils.Refreshable
import com.example.nimons360_far.backend.utils.SessionManager
import com.example.nimons360_far.databinding.ActivityMainBinding
import com.example.nimons360_far.ui.base.BaseActivity
import com.example.nimons360_far.ui.families.CreateFamily
import com.example.nimons360_far.ui.families.Families
import com.example.nimons360_far.ui.home.HomeFragment
import com.example.nimons360_far.ui.map.MapFragment
import com.example.nimons360_far.ui.map.UserLocationData
import android.content.res.ColorStateList
import com.google.android.gms.location.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

class MainActivity : BaseActivity(), SensorEventListener {

    private lateinit var binding: ActivityMainBinding

    val userGeoPoint = MutableStateFlow<GeoPoint?>(null)
    val currentHeading = MutableStateFlow(0f)
    val familyMembers = MutableStateFlow<Map<Int, UserLocationData>>(emptyMap())

    private lateinit var wsManager: MapWebSocketManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var sensorManager: SensorManager
    private var rotationSensor: Sensor? = null
    private lateinit var sessionManager: SessionManager

    private var trackingJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        initTrackingEngine()


        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        updateComponentVisibility(currentFragment)

        if (savedInstanceState == null) {
            loadFragment(HomeFragment(), showNav = true, showFab = true)
        }
    }

    private fun initTrackingEngine() {
        sessionManager = SessionManager(this)
        val token = sessionManager.fetchAuthToken() ?: ""

        wsManager = MapWebSocketManager(token)
        wsManager.connect()
        wsManager.onMemberUpdate = { payload ->
            val updatedList = familyMembers.value.toMutableMap()
            updatedList[payload.userId] = UserLocationData(
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
            familyMembers.value = updatedList
        }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
            startWebSocketLoop()
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            startLocationUpdates()
            startWebSocketLoop()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000).build()
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    userGeoPoint.value = GeoPoint(loc.latitude, loc.longitude)
                }
            }
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    private fun startWebSocketLoop() {
        val userName = sessionManager.fetchUserName() ?: "User"
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        trackingJob = lifecycleScope.launch {
            while (isActive) {
                try {
                    val network = connectivityManager.activeNetwork
                    val capabilities = connectivityManager.getNetworkCapabilities(network)
                    val isMobileData = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
                    val internetStatus = if (isMobileData) "mobile" else "wifi"
                    val batteryLvl = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

                    userGeoPoint.value?.let { myLoc ->
                        wsManager.sendPresence(
                            PresencePayload(
                                name = userName,
                                latitude = myLoc.latitude,
                                longitude = myLoc.longitude,
                                rotation = currentHeading.value,
                                batteryLevel = batteryLvl,
                                isCharging = batteryManager.isCharging,
                                internetStatus = internetStatus
                            )
                        )
                    }
                } catch (e: Exception) {
                    // jika jaringan terputus coba reconnect
                    e.printStackTrace()
                    try {
                        wsManager.connect()
                    } catch (reconnectException: Exception) {
                        reconnectException.printStackTrace()
                    }
                }

                val now = System.currentTimeMillis()
                familyMembers.value = familyMembers.value.filter { (_, member) ->
                    now - member.lastUpdateTime < 5000
                }

                delay(1000)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        rotationSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            val orientationAngles = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            var azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
            azimuth = -azimuth
            if (azimuth < 0) azimuth += 360f
            currentHeading.value = azimuth
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    
    override fun onNetworkAvailable() {
        super.onNetworkAvailable()
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (currentFragment is Refreshable) {
            currentFragment.onRefresh()
        }
    }

    private fun setupUI() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> { loadFragment(HomeFragment(), true, true); true }
                R.id.navigation_map -> { loadFragment(MapFragment(), true, false); true }
                R.id.navigation_families -> { loadFragment(Families(), true, true); true }
                else -> false
            }
        }
        binding.fabAddFamily.setOnClickListener {
            loadFragment(CreateFamily(), showNav = false, showFab = false, addToBackStack = true)
        }
        supportFragmentManager.addOnBackStackChangedListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
            updateComponentVisibility(currentFragment)
        }
    }

    private fun updateComponentVisibility(fragment: Fragment?) {
        when (fragment) {
            is HomeFragment -> showComponent(true, true)
            is MapFragment -> showComponent(true, false)
            is Families -> showComponent(true, true)
            else -> showComponent(false, false)
        }
    }

    private fun showComponent(showNav: Boolean, showFab: Boolean) {
        binding.bottomNavigation.visibility = if (showNav) View.VISIBLE else View.GONE
        binding.fabAddFamily.visibility = if (showFab) View.VISIBLE else View.GONE
    }

    private fun loadFragment(fragment: Fragment, showNav: Boolean, showFab: Boolean, addToBackStack: Boolean = false) {
        showComponent(showNav, showFab)
        val transaction = supportFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment)
        if (addToBackStack) transaction.addToBackStack(null)
        transaction.commit()
    }

    override fun onDestroy() {
        super.onDestroy()
        trackingJob?.cancel()
        if (::wsManager.isInitialized) {
            wsManager.disconnect()
        }
    }
}