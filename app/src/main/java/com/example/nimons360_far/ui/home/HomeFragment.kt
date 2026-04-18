package com.example.nimons360_far.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import android.content.Intent
import com.example.nimons360_far.R
import com.example.nimons360_far.backend.utils.SessionManager
import com.example.nimons360_far.backend.viewmodels.HomeViewModel
import com.example.nimons360_far.databinding.FragmentHomeBinding
import com.example.nimons360_far.ui.auth.ProfileActivity
import com.example.nimons360_far.ui.families.FamilyDetail
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

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

        setupHeader()
        setupRecyclerViews()
        observeViewModel()
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
                Toast.makeText(context, "Please enter family code in details to join", Toast.LENGTH_SHORT).show()
            }
        )
        binding.rvDiscoverFamilies.adapter = discoverAdapter
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.myFamilies.collectLatest { families ->
                myAdapter.updateFamilies(families)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.discoverFamilies.collectLatest { families ->
                discoverAdapter.updateFamilies(families)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
