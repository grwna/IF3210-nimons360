package com.example.nimons360_far.ui.families

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import coil.load
import kotlinx.coroutines.launch
import com.example.nimons360_far.R
import com.example.nimons360_far.ui.viewmodels.CreateEvent
import com.example.nimons360_far.ui.viewmodels.CreateFamilyViewModel
import com.google.android.material.textfield.TextInputEditText

// Fragment - Create Family - Bound To Create Family XML
class CreateFamily : Fragment(R.layout.create_family) {
    // Initialize View Model
    private val viewModel: CreateFamilyViewModel by viewModels()

    // Define Icon URL
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
    private val iconViews = mutableListOf<ImageView>()
    private var ivSelectedIconPreview: ImageView? = null

    // Function - On View Created
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        // Handle Window UI Display
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        // View Bindings
        val etFamilyName = view.findViewById<TextInputEditText>(R.id.etFamilyName)
        val gridIcons = view.findViewById<GridLayout>(R.id.gridIcons)
        val btnHeaderCreate = view.findViewById<TextView>(R.id.btnHeaderCreate)
        val btnCancel = view.findViewById<TextView>(R.id.btnCancel)
        val tvCounter = view.findViewById<TextView>(R.id.tvCounter)
        ivSelectedIconPreview = view.findViewById(R.id.ivSelectedIconPreview)

        // Initialize Dynamic Icon Grid
        setupIconGrid(gridIcons)

        // Handle Back Navigation - On Click
        btnCancel.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        // Handle Create - On Click
        btnHeaderCreate.setOnClickListener {
            val familyName = etFamilyName.text.toString().trim()
            viewModel.attemptCreateFamily(familyName)
        }

        // Handle Character Counter - Before-On-After Change
        tvCounter.text = "${etFamilyName.text?.length ?: 0}/40"
        etFamilyName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                tvCounter.text = "${s?.length ?: 0}/40"
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Observe View Model State
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe Selected Icon Changes
                launch {
                    viewModel.selectedIconUrl.collect { url ->
                        if (url != null) {
                            ivSelectedIconPreview?.load(url) {
                                crossfade(true)
                            }
                        }
                    }
                }

                // Observe Success / Error Events
                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is CreateEvent.Success -> {
                                // Success Create Family
                                Toast.makeText(requireContext(), "Family '${event.familyName}' created!", Toast.LENGTH_SHORT).show()

                                // Navigate To Family Detail
                                val familyDetail = FamilyDetail().apply {
                                    arguments = Bundle().apply { putInt("familyId", event.newFamilyId) }
                                }
                                val fragmentManager = requireActivity().supportFragmentManager
                                fragmentManager.popBackStack()
                                requireActivity().supportFragmentManager.beginTransaction()
                                    .replace(R.id.fragment_container, familyDetail)
                                    .addToBackStack(null)
                                    .commit()
                            }
                            is CreateEvent.Error -> {
                                // Failed Create Family
                                Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }

    // Function - Setup Icon Grid
    private fun setupIconGrid(gridIcons: GridLayout) {
        // General Setting
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val paddingTotal = (48 * displayMetrics.density).toInt()
        val availableWidth = screenWidth - paddingTotal
        val cellSize = (availableWidth / 5)
        val margin = (4 * displayMetrics.density).toInt()
        val imageSize = cellSize - (margin * 2)

        // Icon Setting
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
                setOnClickListener {
                    viewModel.selectIcon(url)
                    updateIconSelectionUI(this)
                }
            }

            // Set Display
            iconViews.add(imageView)
            gridIcons.addView(imageView)
        }

        // Auto Select First Icon
        if (iconViews.isNotEmpty()) {
            iconViews[0].performClick()
        }
    }

    // Function - Update Borders & Backgrounds Of Selected Icon
    private fun updateIconSelectionUI(selectedView: ImageView) {
        // Unselected Icon
        val unselectedBg = GradientDrawable().apply {
            setColor(com.example.nimons360_far.utils.AppColors.DesignBgInt)
            cornerRadius = 16f * resources.displayMetrics.density
        }

        // Selected Icon
        val selectedBg = GradientDrawable().apply {
            setColor(com.example.nimons360_far.utils.AppColors.LightBlueBgInt)
            setStroke((2 * resources.displayMetrics.density).toInt(), com.example.nimons360_far.utils.AppColors.DesignBlueInt)
            cornerRadius = 16f * resources.displayMetrics.density
        }

        // Set Display
        iconViews.forEach {
            it.background = unselectedBg
        }
        selectedView.background = selectedBg
    }
}