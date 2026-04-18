package com.example.nimons360_far.ui.base

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.nimons360_far.backend.utils.NetworkObserver
import com.example.nimons360_far.backend.utils.navigateTo
import com.example.nimons360_far.ui.auth.LoginActivity
import com.example.nimons360_far.ui.components.NoInternetDialog
import kotlinx.coroutines.launch

// ABC for common functionality for all activities
abstract class BaseActivity : AppCompatActivity() {

    private var noInternetDialog: NoInternetDialog? = null
    private lateinit var networkObserver: NetworkObserver

    // forced logout events receiver from interceptor
    private val logoutReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Clear caches
            context?.let {
                com.example.nimons360_far.backend.utils.UserRepository.getInstance(it).clearCache()
            }
            if (this@BaseActivity !is LoginActivity) {
                navigateTo(LoginActivity::class.java, clearStack = true)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO) // Force light mode
        networkObserver = NetworkObserver.getInstance(this)
        registerLogoutReceiver()
        observeNetwork()
    }

    // Network Sensing
    private fun observeNetwork() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                networkObserver.isConnected.collect { isConnected ->
                    if (isConnected) {
                        val wasOffline = noInternetDialog != null
                        dismissNoInternetDialog()
                        if (wasOffline) {
                            onNetworkAvailable()
                        }
                    } else {
                        showNoInternetDialog()
                    }
                }
            }
        }
    }

    private fun showNoInternetDialog() {
        if (noInternetDialog == null) {
            noInternetDialog = NoInternetDialog.newInstance()
            noInternetDialog?.show(supportFragmentManager, NoInternetDialog.TAG)
        }
    }

    private fun dismissNoInternetDialog() {
        noInternetDialog?.dismiss()
        noInternetDialog = null
    }

    // Internet restoration behaviour
    open fun onNetworkAvailable() {}

    private fun registerLogoutReceiver() {
        val filter = IntentFilter("com.example.nimons360_far.ACTION_LOGOUT")
        // handle Tiramisu+ receiver safety rules
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(logoutReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(logoutReceiver, filter)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(logoutReceiver)
        } catch (e: Exception) { }
    }
}
