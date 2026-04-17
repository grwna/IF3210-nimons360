package com.example.nimons360_far.ui.screens

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.nimons360_far.LauncherActivity
import com.example.nimons360_far.backend.utils.SessionManager
import com.example.nimons360_far.backend.viewmodels.ProfileState
import com.example.nimons360_far.backend.viewmodels.ProfileViewModel
import com.example.nimons360_far.databinding.ActivityProfileBinding
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var profileViewModel: ProfileViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        profileViewModel = ViewModelProvider(this).get(ProfileViewModel::class.java)

        ViewCompat.setOnApplyWindowInsetsListener(binding.profileRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sessionManager = SessionManager(this)

        // listen for profile state updates
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                profileViewModel.profileState.collect { state ->
                    when (state) {
                        is ProfileState.Idle -> { }
                        is ProfileState.Loading -> {
                            binding.tvProfileName.text = "Loading..."
                        }
                        is ProfileState.Success -> {
                            val data = state.profile
                            binding.tvProfileName.text = data.fullName
                            binding.tvProfileEmail.text = data.email
                            binding.tvAvatarInitial.text = data.fullName.substring(0, 1).uppercase()
                        }
                        is ProfileState.Error -> {
                            Toast.makeText(this@ProfileActivity, state.message, Toast.LENGTH_SHORT).show()
                            // fallback to SessionManager's saved user data
                            val name = sessionManager.fetchUserName() ?: "Unknown User"
                            val email = sessionManager.fetchUserEmail() ?: "user@example.com"
                            binding.tvProfileName.text = name
                            binding.tvProfileEmail.text = email
                            binding.tvAvatarInitial.text = name.substring(0, 1).uppercase()
                        }
                    }
                }
            }
        }

        // Fetch data (will use cache if available)
        profileViewModel.fetchProfile()

        // Deferred Bottom Sheet trigger
        binding.ivEditPencil.setOnClickListener {
            Toast.makeText(this, "Editing coming soon (waiting for Figma design)", Toast.LENGTH_SHORT).show()
        }

        binding.btnSignOut.setOnClickListener {
            sessionManager.clearSession()
            
            // Broadcast logout for other logout handlers
            val intent = Intent("com.example.nimons360_far.ACTION_LOGOUT")
            intent.setPackage(packageName)
            sendBroadcast(intent)
            
            // Redirect to login
            val launchIntent = Intent(this, LauncherActivity::class.java)
            launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(launchIntent)
            finish()
        }
    }
}

