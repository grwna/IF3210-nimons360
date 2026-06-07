package com.example.nimons360_far.ui.auth

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import coil.request.CachePolicy
import com.example.nimons360_far.R
import com.example.nimons360_far.backend.utils.SessionManager
import com.example.nimons360_far.ui.viewmodels.ProfileState
import com.example.nimons360_far.ui.viewmodels.ProfileViewModel
import com.example.nimons360_far.databinding.ActivityProfileBinding
import com.example.nimons360_far.ui.base.BaseActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.messaging.FirebaseMessaging
import android.util.Log
import kotlinx.coroutines.launch

class ProfileActivity : BaseActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var profileViewModel: ProfileViewModel

    private var tempImageUri: Uri? = null

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
        if (success) {
            tempImageUri?.let { uri ->
                profileViewModel.uploadProfilePhoto(uri, this)
            }
        }
    }

    // Camera permission
    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            tempImageUri = getTempImageUri()
            takePictureLauncher.launch(tempImageUri!!)
        } else {
            android.widget.Toast.makeText(this, "Camera permission is required to take a photo.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            profileViewModel.uploadProfilePhoto(it, this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        profileViewModel = ViewModelProvider(this)[ProfileViewModel::class.java]

        ViewCompat.setOnApplyWindowInsetsListener(binding.profileRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sessionManager = SessionManager(this)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                profileViewModel.profileState.collect { state ->
                    when (state) {
                        is ProfileState.Idle -> { }
                        is ProfileState.Loading -> {
                            binding.tvProfileName.text = "Loading..."
                            binding.loadingIndicator.visibility = View.VISIBLE
                            binding.tvError.visibility = View.GONE
                        }
                        is ProfileState.Success -> {
                            binding.loadingIndicator.visibility = View.GONE
                            binding.tvError.visibility = View.GONE
                            val data = state.profile
                            binding.tvProfileName.text = data.fullName
                            binding.tvProfileEmail.text = data.email
                            binding.tvAvatarInitial.text = data.fullName.substring(0, 1).uppercase()

                            val rawUrl = data.profileImageUrl
                            val finalUrl = if (rawUrl?.startsWith("/") == true) {
                                "https://mad.labpro.hmif.dev$rawUrl"
                            } else {
                                rawUrl
                            }

                            if (!finalUrl.isNullOrEmpty()) {
                                binding.ivProfilePhoto.visibility = View.VISIBLE
                                binding.ivProfilePhoto.load(finalUrl) {
                                    crossfade(true)
                                    memoryCachePolicy(CachePolicy.DISABLED)
                                    diskCachePolicy(CachePolicy.DISABLED)
                                    listener(onError = { _, _ -> binding.ivProfilePhoto.visibility = View.GONE })
                                }
                            } else {
                                binding.ivProfilePhoto.visibility = View.GONE
                            }
                        }
                        is ProfileState.Error -> {
                            binding.loadingIndicator.visibility = View.GONE
                            binding.tvError.visibility = View.GONE
                            android.widget.Toast.makeText(this@ProfileActivity, state.message, android.widget.Toast.LENGTH_SHORT).show()
                            
                            // fallback to locally saved session if API fetch fails
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

        // Init toggles
        val switchNotification = findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.switch_notification)
        val switchLocationPrivacy = findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.switch_location_privacy)

        switchNotification.isChecked = sessionManager.fetchNotificationPreference()
        switchLocationPrivacy.isChecked = sessionManager.fetchLocationPrivacyPreference()

        switchNotification.setOnCheckedChangeListener { _, isChecked ->
            sessionManager.saveNotificationPreference(isChecked)
            
            val notificationApi = com.example.nimons360_far.backend.network.ApiClient.getNotificationApi(this@ProfileActivity)
            
            if (isChecked) {
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val token = task.result
                        lifecycleScope.launch {
                            try {
                                notificationApi.subscribe(com.example.nimons360_far.backend.models.SubscribeRequest(token))
                            } catch (e: Exception) {
                                Log.e("ProfileActivity", "Failed to subscribe", e)
                            }
                        }
                    }
                }
            } else {
                lifecycleScope.launch {
                    try {
                        notificationApi.unsubscribe()
                    } catch (e: Exception) {
                        Log.e("ProfileActivity", "Failed to unsubscribe", e)
                    }
                }
            }
        }

        switchLocationPrivacy.setOnCheckedChangeListener { _, isChecked ->
            sessionManager.saveLocationPrivacyPreference(isChecked)
        }

        // Fetch data (with cache if avail)
        profileViewModel.fetchProfile()

        // Bottom Sheet trigger
        binding.ivEditPencil.setOnClickListener {
            val options = arrayOf("Take Photo", "Choose from Gallery")
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Update Profile Picture")
                .setItems(options) { dialog, which ->
                    when (which) {
                        0 -> {
                            if (androidx.core.content.ContextCompat.checkSelfPermission(this@ProfileActivity, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                tempImageUri = getTempImageUri()
                                takePictureLauncher.launch(tempImageUri!!)
                            } else {
                                requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                            }
                        }
                        1 -> pickImageLauncher.launch("image/*")
                    }
                    dialog.dismiss()
                }
                .show()
        }

        binding.tvProfileName.setOnClickListener {
            showEditNameBottomSheet()
        }

        binding.ivEditNamePencil.setOnClickListener {
            showEditNameBottomSheet()
        }

        // Sign Out Modal Trigger
        binding.btnSignOut.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            val dialogView = layoutInflater.inflate(R.layout.dialog_sign_out, null)
            builder.setView(dialogView)
            val dialog = builder.create()
            dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

            val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btn_cancel)
            val btnSignOutConfirm = dialogView.findViewById<MaterialButton>(R.id.btn_sign_out_confirm)
            val tvDialogAvatar = dialogView.findViewById<TextView>(R.id.tv_dialog_avatar_initial)

            tvDialogAvatar.text = binding.tvAvatarInitial.text

            btnCancel.setOnClickListener {
                dialog.dismiss()
            }

            btnSignOutConfirm.setOnClickListener {
                dialog.dismiss()
                sessionManager.clearSession()
                
                // broadcast logout to be handled globally by BaseActivity
                val intent = Intent("com.example.nimons360_far.ACTION_LOGOUT")
                intent.setPackage(packageName)
                sendBroadcast(intent)
            }

            dialog.show()
        }
    }

    // Camera functionality
    private fun getTempImageUri(): Uri {
        val tempFile = java.io.File.createTempFile("avatar_temp", ".jpg", java.io.File(cacheDir, "shared_files").apply { mkdirs() }).apply {
            createNewFile()
            deleteOnExit()
        }
        return androidx.core.content.FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            tempFile
        )
    }

    private fun showEditNameBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_edit_name, null)
        bottomSheetDialog.setContentView(sheetView)
        
        // Set background transparent to show curved top corners
        (sheetView.parent as? View)?.setBackgroundColor(Color.TRANSPARENT)

        val etName = sheetView.findViewById<TextInputEditText>(R.id.et_name)
        val btnCancel = sheetView.findViewById<MaterialButton>(R.id.btn_cancel)
        val btnSave = sheetView.findViewById<MaterialButton>(R.id.btn_save)

        val tilName = sheetView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.til_name)

        // Populate current name
        etName.setText(binding.tvProfileName.text.toString())

        btnCancel.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        btnSave.setOnClickListener {
            val newName = etName.text.toString().trim()
            tilName.error = null

            if (newName.isEmpty()) {
                tilName.error = "Name cannot be empty"
            } else if (newName.length > 50) {
                tilName.error = "Name is too long (max 50 characters)"
            } else {
                profileViewModel.updateProfile(newName)
                bottomSheetDialog.dismiss()
            }
        }

        bottomSheetDialog.show()
    }

    override fun onNetworkAvailable() {
        profileViewModel.fetchProfile(forceRefresh = true)
    }
}


