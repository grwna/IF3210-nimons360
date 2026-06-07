package com.example.nimons360_far.ui.auth

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.nimons360_far.LauncherActivity
import com.example.nimons360_far.R
import com.example.nimons360_far.backend.models.LoginRequest
import com.example.nimons360_far.backend.utils.navigateTo
import com.example.nimons360_far.ui.viewmodels.AuthState
import com.example.nimons360_far.ui.viewmodels.AuthViewModel
import com.example.nimons360_far.databinding.ActivityLoginBinding
import com.example.nimons360_far.ui.base.BaseActivity
import kotlinx.coroutines.launch

class LoginActivity : BaseActivity() {

    private lateinit var authViewModel: AuthViewModel
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // initialize ViewModel and set up layout with View Binding components
        authViewModel = ViewModelProvider(this).get(AuthViewModel::class.java)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.login) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Login Sequence
        binding.loginButton.setOnClickListener {
            val email = binding.loginEmailInput.text.toString().trim()
            val password = binding.editTextTextPassword.text.toString().trim()

            binding.emailInputLayout.error = null
            binding.passwordInputLayout.error = null

            var isValid = true

            // Email validation
            if (email.isEmpty()) {
                binding.emailInputLayout.error = "Email is required"
                isValid = false
            } else if (email.length > 320) {
                binding.emailInputLayout.error = "Email is too long (max 320 characters)"
                isValid = false
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.emailInputLayout.error = "Invalid email format"
                isValid = false
            }

            // Password validation
            if (password.isEmpty()) {
                binding.passwordInputLayout.error = "Password is required"
                isValid = false
            } else if (password.length < 8) {
                binding.passwordInputLayout.error = "Password must be at least 8 characters"
                isValid = false
            } else if (password.length > 128) {
                binding.passwordInputLayout.error = "Password is too long (max 128 characters)"
                isValid = false
            }

            if (isValid) {
                authViewModel.login(LoginRequest(email, password))
            }
        }

        // observes auth state data and updates UI accordingly
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.authState.collect { state ->
                    when (state) {
                        is AuthState.Idle -> {
                            binding.loginButton.isEnabled = true
                            binding.loginButton.text = getString(R.string.login_button_text)
                        }
                        is AuthState.Loading -> {
                            binding.loginButton.isEnabled = false
                            binding.loginButton.text = "Loading..."
                        }
                        is AuthState.Success -> {
                            binding.loginButton.isEnabled = true
                            binding.loginButton.text = "Success!"
                            // redirect to Launcher to handle session routing
                            navigateTo(LauncherActivity::class.java, clearStack = true)
                        }
                        is AuthState.Error -> {
                            binding.loginButton.isEnabled = true
                            binding.loginButton.text = getString(R.string.login_button_text)
                            // Show server error on email field
                            binding.emailInputLayout.error = state.message
                        }
                    }
                }
            }
        }
    }
}

