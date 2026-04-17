package com.example.nimons360_far.ui.base

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.nimons360_far.backend.utils.navigateTo
import com.example.nimons360_far.ui.screens.LoginActivity

// ABC for common functionality for all activities
abstract class BaseActivity : AppCompatActivity() {

    // forced logout events receiver from interceptor
    private val logoutReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (this@BaseActivity !is LoginActivity) {
                navigateTo(LoginActivity::class.java, clearStack = true)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerLogoutReceiver()
    }

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
