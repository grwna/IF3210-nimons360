package com.example.nimons360_far.ui.families

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import android.content.Context
import coil.load
import com.example.nimons360_far.R
import com.example.nimons360_far.api.FamilyAPI
import com.example.nimons360_far.model.CreateFamilyRequest
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CreateFamily : Fragment(R.layout.create_family) {
    private val iconUrls = listOf(
        "https://mad.labpro.hmif.dev/assets/family_icon_1.png",
        "https://mad.labpro.hmif.dev/assets/family_icon_2.png",
        "https://mad.labpro.hmif.dev/assets/family_icon_3.png",
        "https://mad.labpro.hmif.dev/assets/family_icon_4.png",
        "https://mad.labpro.hmif.dev/assets/family_icon_5.png",
        "https://mad.labpro.hmif.dev/assets/family_icon_6.png",
        "https://mad.labpro.hmif.dev/assets/family_icon_7.png",
        "https://mad.labpro.hmif.dev/assets/family_icon_8.png"
    )
    private var selectedIconUrl: String? = null
    private val iconViews = mutableListOf<ImageView>()
    private var ivSelectedIconPreview: ImageView? = null

    private val api: FamilyAPI by lazy {
        Retrofit.Builder()
            .baseUrl("https://mad.labpro.hmif.dev")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FamilyAPI::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val etFamilyName = view.findViewById<TextInputEditText>(R.id.etFamilyName)
        val gridIcons = view.findViewById<GridLayout>(R.id.gridIcons)
        val btnHeaderCreate = view.findViewById<TextView>(R.id.btnHeaderCreate)
        val btnCancel = view.findViewById<TextView>(R.id.btnCancel)
        ivSelectedIconPreview = view.findViewById(R.id.ivSelectedIconPreview)
        setupIconGrid(gridIcons)
        btnCancel.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
        btnHeaderCreate.setOnClickListener {
            attemptCreateFamily(etFamilyName)
        }
    }

    private fun attemptCreateFamily(etFamilyName: TextInputEditText) {
        val familyName = etFamilyName.text.toString().trim()
        if (familyName.isEmpty()) {
            Toast.makeText(requireContext(), "Family name cannot be empty!", Toast.LENGTH_SHORT)
                .show()
            return
        }
        if (familyName.length > 40) {
            Toast.makeText(requireContext(), "Family name is too long!", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedIconUrl == null) {
            Toast.makeText(requireContext(), "Please select an icon!", Toast.LENGTH_SHORT).show()
            return
        }
        val sharedPreferences = requireContext().getSharedPreferences("Nimons360Prefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("AUTH_TOKEN", "") ?: ""
        val bearerToken = "Bearer $token"

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val request = CreateFamilyRequest(familyName, selectedIconUrl!!)
                val response = api.createFamily(bearerToken, request)
                withContext(Dispatchers.Main) {
                    if ((response.isSuccessful) && (response.body() != null)) {
                        val newFamily = response.body()!!.data
                        Toast.makeText(
                            requireContext(),
                            "Family '${newFamily.name}' created!",
                            Toast.LENGTH_SHORT
                        ).show()
                        val familyDetail = FamilyDetail().apply {
                            arguments = Bundle().apply {
                                putInt("familyId", newFamily.id)
                            }
                        }
                        requireActivity().supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, familyDetail)
                            .addToBackStack(null)
                            .commit()
                    } else {
                        val errorMsg =
                            if (response.code() == 409) {
                                "Token expired, please re-login"
                            } else {
                                "Failed to create family"
                            }
                        Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    private fun setupIconGrid(gridIcons: GridLayout) {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val paddingTotal = (48 * displayMetrics.density).toInt()
        val availableWidth = screenWidth - paddingTotal
        val cellSize = (availableWidth / 5)
        val margin = (4 * displayMetrics.density).toInt()
        val imageSize = cellSize - (margin * 2)
        for (url in iconUrls) {
            val imageView = ImageView(requireContext()).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = imageSize
                    height = imageSize
                    setMargins(margin, margin, margin, margin)
                }
                val innerPadding = (12 * displayMetrics.density).toInt()
                setPadding(innerPadding, innerPadding, innerPadding, innerPadding)
                load(url) { crossfade(true) }
                setOnClickListener { selectIcon(this, url) }
            }
            iconViews.add(imageView)
            gridIcons.addView(imageView)
        }
        if (iconViews.isNotEmpty()) {
            iconViews[0].performClick()
        }
    }

    private fun selectIcon(selectedView: ImageView, url: String) {
        selectedIconUrl = url
        ivSelectedIconPreview?.load(url) { crossfade(true) }
        val unselectedBg = GradientDrawable().apply {
            setColor(Color.parseColor("#F8F9FA"))
            cornerRadius = 16f * resources.displayMetrics.density
        }
        val selectedBg = GradientDrawable().apply {
            setColor(Color.parseColor("#EAF2FF"))
            setStroke((2 * resources.displayMetrics.density).toInt(), Color.parseColor("#1868D8"))
            cornerRadius = 16f * resources.displayMetrics.density
        }
        iconViews.forEach { it.background = unselectedBg }
        selectedView.background = selectedBg
    }
}