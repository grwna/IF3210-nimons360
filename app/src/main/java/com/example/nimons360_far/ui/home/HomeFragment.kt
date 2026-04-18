package com.example.nimons360_far.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.nimons360_far.R
import com.example.nimons360_far.backend.utils.SessionManager
import com.example.nimons360_far.backend.utils.Refreshable
import com.example.nimons360_far.backend.viewmodels.HomeViewModel
import com.example.nimons360_far.databinding.FragmentHomeBinding
import com.example.nimons360_far.ui.auth.ProfileActivity
import com.example.nimons360_far.ui.families.FamilyDetail
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeFragment : Fragment(), Refreshable {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HomeViewModel
    
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

        // Notification Bar Inset
        ViewCompat.setOnApplyWindowInsetsListener(binding.homeHeader) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Preserve horizontal padding
            val leftPadding = v.paddingLeft
            val rightPadding = v.paddingRight
            val bottomPadding = v.paddingBottom
            v.setPadding(leftPadding, bars.top, rightPadding, bottomPadding)
            insets
        }

        setupRecyclerViews()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        setupHeader()
    }

    private fun setupHeader() {
        val sessionManager = SessionManager(requireContext())
        val name = sessionManager.fetchUserName() ?: "U"
        if (name.isNotEmpty()) {
            binding.tvHomeAvatar.text = name.substring(0, 1).uppercase()
        }

        binding.tvHomeAvatar.setOnClickListener {
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
                binding.loadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
                binding.homeContent.alpha = if (isLoading) 0.5f else 1.0f
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
