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
import com.example.nimons360_far.R
import com.example.nimons360_far.backend.models.LoginRequest
import com.example.nimons360_far.backend.viewmodels.AuthState
import com.example.nimons360_far.backend.viewmodels.AuthViewModel
import com.example.nimons360_far.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var authViewModel: AuthViewModel
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize ViewModel and set up layout with View Binding
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

            if (email.isNotEmpty() && password.isNotEmpty()) {
                authViewModel.login(LoginRequest(email, password))
            } else {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            }
        }

        // Observes ViewModel state and update UI based on the state
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
                            val intent = Intent(this@LoginActivity, LauncherActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                        is AuthState.Error -> {
                            binding.loginButton.isEnabled = true
                            binding.loginButton.text = getString(R.string.login_button_text)
                            Toast.makeText(this@LoginActivity, state.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
}
