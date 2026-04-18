package com.example.nimons360_far

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.nimons360_far.backend.utils.SessionManager
import com.example.nimons360_far.backend.utils.navigateTo
import com.example.nimons360_far.ui.main.MainActivity
import com.example.nimons360_far.ui.auth.LoginActivity

// Entry point of app
class LauncherActivity : AppCompatActivity() {
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(this)

        // determine destination based on session
        if (sessionManager.fetchAuthToken() == null) {
            navigateTo(LoginActivity::class.java, clearStack = true)
        } else {
            navigateTo(MainActivity::class.java, clearStack = true)
        }
        finish()
    }
}