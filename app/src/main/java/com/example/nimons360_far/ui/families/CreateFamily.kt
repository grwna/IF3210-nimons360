package com.example.nimons360_far.ui.families

import android.graphics.Color
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
import com.example.nimons360_far.R
import com.example.nimons360_far.backend.viewmodels.CreateEvent
import com.example.nimons360_far.backend.viewmodels.CreateFamilyViewModel
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class CreateFamily : Fragment(R.layout.create_family) {

    private val viewModel: CreateFamilyViewModel by viewModels()

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
            insets
        }
        val etFamilyName = view.findViewById<TextInputEditText>(R.id.etFamilyName)
        val gridIcons = view.findViewById<GridLayout>(R.id.gridIcons)
        val btnHeaderCreate = view.findViewById<TextView>(R.id.btnHeaderCreate)
        val btnCancel = view.findViewById<TextView>(R.id.btnCancel)
        val tvCounter = view.findViewById<TextView>(R.id.tvCounter)
        ivSelectedIconPreview = view.findViewById(R.id.ivSelectedIconPreview)

        setupIconGrid(gridIcons)

        btnCancel.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        btnHeaderCreate.setOnClickListener {
            val familyName = etFamilyName.text.toString().trim()
            viewModel.attemptCreateFamily(familyName)
        }

        tvCounter.text = "${etFamilyName.text?.length ?: 0}/40"
        etFamilyName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                tvCounter.text = "${s?.length ?: 0}/40"
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.selectedIconUrl.collect { url ->
                        if (url != null) {
                            ivSelectedIconPreview?.load(url) { crossfade(true) }
                        }
                    }
                }
                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is CreateEvent.Success -> {
                                Toast.makeText(requireContext(), "Family '${event.familyName}' created!", Toast.LENGTH_SHORT).show()
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
                                Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
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
                setOnClickListener {
                    viewModel.selectIcon(url)
                    updateIconSelectionUI(this)
                }
            }
            iconViews.add(imageView)
            gridIcons.addView(imageView)
        }
        if (iconViews.isNotEmpty()) {
            iconViews[0].performClick()
        }
    }

    private fun updateIconSelectionUI(selectedView: ImageView) {
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