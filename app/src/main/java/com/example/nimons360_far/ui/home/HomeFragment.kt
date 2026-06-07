package com.example.nimons360_far.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.content.Intent
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.nimons360_far.R
import com.example.nimons360_far.backend.utils.SessionManager
import com.example.nimons360_far.backend.utils.Refreshable
import com.example.nimons360_far.ui.viewmodels.HomeViewModel
import com.example.nimons360_far.ui.viewmodels.ProfileViewModel
import com.example.nimons360_far.ui.viewmodels.ProfileState
import com.example.nimons360_far.databinding.FragmentHomeBinding
import com.example.nimons360_far.ui.auth.ProfileActivity
import com.example.nimons360_far.ui.families.FamilyDetail
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import coil.load
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeFragment : Fragment(), Refreshable {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HomeViewModel
    private lateinit var profileViewModel: ProfileViewModel
    private lateinit var myAdapter: MyFamilyAdapter
    private lateinit var discoverAdapter: DiscoverFamilyAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        profileViewModel = ViewModelProvider(requireActivity())[ProfileViewModel::class.java]

        // Notification Bar Inset
        ViewCompat.setOnApplyWindowInsetsListener(binding.homeHeader) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val density = v.resources.displayMetrics.density
            val horizontalPadding = (20 * density).toInt()
            val bottomPadding = (12 * density).toInt()
            v.setPadding(horizontalPadding + bars.left, bars.top, horizontalPadding + bars.right, bottomPadding)
            insets
        }

        setupRecyclerViews()
        setupSwipeRefresh()
        observeViewModel()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(R.color.design_blue)
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshAll()
            profileViewModel.fetchProfile(forceRefresh = true)
        }
    }

    override fun onResume() {
        super.onResume()
        setupHeader()
        profileViewModel.fetchProfile()
    }

    private fun setupHeader() {
        val sessionManager = SessionManager(requireContext())
        val name = sessionManager.fetchUserName() ?: "U"
        if (name.isNotEmpty()) {
            binding.tvHomeAvatarInitial.text = name.substring(0, 1).uppercase()
        }

        binding.cvHomeAvatar.setOnClickListener {
            val intent = Intent(requireContext(), ProfileActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupRecyclerViews() {
        myAdapter = MyFamilyAdapter(emptyList()) { family ->
            navigateToDetail(family.id)
        }
        binding.rvMyFamilies.adapter = myAdapter

        discoverAdapter = DiscoverFamilyAdapter(emptyList(), 
            onItemClick = { family -> navigateToDetail(family.id) },
            onJoinClick = { family -> 
                navigateToDetail(family.id)
            }
        )
        binding.rvDiscoverFamilies.adapter = discoverAdapter
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            profileViewModel.profileState.collectLatest { state ->
                if (state is ProfileState.Success) {
                    val data = state.profile
                    if (data.fullName.isNotEmpty()) {
                        binding.tvHomeAvatarInitial.text = data.fullName.substring(0, 1).uppercase()
                    }
                    val rawUrl = data.profileImageUrl
                    val finalUrl = if (rawUrl?.startsWith("/") == true) "https://mad.labpro.hmif.dev$rawUrl" else rawUrl
                    if (!finalUrl.isNullOrEmpty()) {
                        binding.ivHomeAvatar.visibility = View.VISIBLE
                        binding.ivHomeAvatar.load(finalUrl) {
                            crossfade(true)
                            listener(onError = { _, _ -> binding.ivHomeAvatar.visibility = View.GONE })
                        }
                    } else {
                        binding.ivHomeAvatar.visibility = View.GONE
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.myFamilies.collectLatest { families ->
                myAdapter.updateFamilies(families)
                binding.tvEmptyMyFamilies.visibility = if (families.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.discoverFamilies.collectLatest { families ->
                discoverAdapter.updateFamilies(families)
                binding.tvEmptyDiscoverFamilies.visibility = if (families.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                binding.loadingIndicator.visibility = if (isLoading && !binding.swipeRefresh.isRefreshing) View.VISIBLE else View.GONE
                binding.swipeRefresh.isRefreshing = isLoading
                binding.swipeRefresh.alpha = if (isLoading) 0.5f else 1.0f
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collectLatest { error ->
                error?.let {
                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun navigateToDetail(familyId: Int) {
        val familyDetail = FamilyDetail().apply {
            arguments = Bundle().apply { putInt("familyId", familyId) }
        }
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, familyDetail)
            .addToBackStack(null)
            .commit()
    }

    override fun onRefresh() {
        viewModel.refreshAll()
        profileViewModel.fetchProfile(forceRefresh = true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
