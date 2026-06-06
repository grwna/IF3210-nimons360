package com.example.nimons360_far.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.nimons360_far.R
import com.example.nimons360_far.backend.utils.Refreshable
import com.example.nimons360_far.backend.utils.SessionManager
import com.example.nimons360_far.databinding.ActivityMainBinding
import com.example.nimons360_far.ui.base.BaseActivity
import com.example.nimons360_far.ui.families.CreateFamily
import com.example.nimons360_far.ui.families.Families
import com.example.nimons360_far.ui.home.HomeFragment
import com.example.nimons360_far.ui.map.MapFragment
import com.example.nimons360_far.ui.map.UserLocationData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sessionManager: SessionManager

    val userGeoPoint = MutableStateFlow<GeoPoint?>(null)
    val currentHeading = MutableStateFlow(0f)
    val familyMembers = MutableStateFlow<Map<Int, UserLocationData>>(emptyMap())

    // Service binding
    private var trackingService: LocationTrackingService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as LocationTrackingService.LocalBinder
            trackingService = localBinder.getService()
            isBound = true
            // Mulai mirror StateFlow dari service ke MainActivity
            observeServiceFlows()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            trackingService = null
            isBound = false
        }
    }

    // Handle notification preference on launch
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            android.util.Log.d("MainActivity", "Notification permission granted")
        } else {
            android.util.Log.d("MainActivity", "Notification permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sessionManager = SessionManager(this)

        // Request notification permission
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Auto subscribe on launch if notif is enabled
        if (sessionManager.fetchNotificationPreference()) {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val fcmToken = task.result
                    lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val notificationApi = com.example.nimons360_far.backend.network.ApiClient.getNotificationApi(this@MainActivity)
                            notificationApi.subscribe(com.example.nimons360_far.backend.models.SubscribeRequest(fcmToken))
                            android.util.Log.d("MainActivity", "FCM token subscribed successfully on startup")
                        } catch (e: java.lang.Exception) {
                            android.util.Log.e("MainActivity", "Failed to subscribe FCM token on startup", e)
                        }
                    }
                }
            }
        }

        setupUI()

        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        updateComponentVisibility(currentFragment)

        if (savedInstanceState == null) {
            loadFragment(HomeFragment(), showNav = true, showFab = true)
        }

        // Cek permission lokasi, jika sudah ada langsung start & bind service
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startAndBindService()
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    override fun onStart() {
        super.onStart()
        // Bind ke service yang sudah berjalan jika belum terbind
        if (!isBound) {
            val intent = Intent(this, LocationTrackingService::class.java)
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        // Unbind saat activity tidak visible, tapi service tetap jalan karena startForegroundService
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
            trackingService = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Service hanya berhenti jika user force stop
    }

    @SuppressLint("MissingPermission")
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startAndBindService()
            }
        }

    // Start service (jika belum berjalan) dan bind ke service
    // startForegroundService memastikan service tetap hidup di background
    private fun startAndBindService() {
        LocationTrackingService.start(this)
        val intent = Intent(this, LocationTrackingService::class.java)
        if (!isBound) {
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    // Kumpulkan StateFlow dari service ke StateFlow MainActivity buat MapFragment 
    private fun observeServiceFlows() {
        val service = trackingService ?: return
        lifecycleScope.launch {
            service.userGeoPoint.collectLatest { userGeoPoint.value = it }
        }
        lifecycleScope.launch {
            service.currentHeading.collectLatest { currentHeading.value = it }
        }
        lifecycleScope.launch {
            service.familyMembers.collectLatest { familyMembers.value = it }
        }
    }

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
        binding.fabAddFamily.visibility = if (showFab) View.GONE else View.VISIBLE
    }

    private fun loadFragment(fragment: Fragment, showNav: Boolean, showFab: Boolean, addToBackStack: Boolean = false) {
        showComponent(showNav, showFab)
        val transaction = supportFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment)
        if (addToBackStack) transaction.addToBackStack(null)
        transaction.commit()
    }
}