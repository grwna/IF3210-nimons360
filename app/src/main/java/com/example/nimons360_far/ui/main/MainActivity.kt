package com.example.nimons360_far.ui.main

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.nimons360_far.R
import com.example.nimons360_far.databinding.ActivityMainBinding
import com.example.nimons360_far.backend.utils.Refreshable
import com.example.nimons360_far.ui.base.BaseActivity
import com.example.nimons360_far.ui.families.CreateFamily
import com.example.nimons360_far.ui.families.Families
import com.example.nimons360_far.ui.home.HomeFragment
import com.example.nimons360_far.ui.map.MapFragment

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        
        if (savedInstanceState == null) {
            loadFragment(HomeFragment(), showNav = true, showFab = true)
        }
    }

    override fun onNetworkAvailable() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (currentFragment is Refreshable) {
            currentFragment.onRefresh()
        }
    }

    private fun setupUI() {
        // Navbar
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    loadFragment(HomeFragment(), showNav = true, showFab = true)
                    true
                }
                R.id.navigation_map -> {
                    loadFragment(MapFragment(), showNav = true, showFab = false)
                    true
                }
                R.id.navigation_families -> {
                    loadFragment(Families(), showNav = true, showFab = true)
                    true
                }
                else -> false
            }
        }

        // Create family button
        binding.fabAddFamily.setOnClickListener {
            loadFragment(CreateFamily(), showNav = false, showFab = false, addToBackStack = true)
        }

        // Handle back stack changes to restore chrome visibility
        supportFragmentManager.addOnBackStackChangedListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
            updateComponentVisibility(currentFragment)
        }
    }

    private fun updateComponentVisibility(fragment: Fragment?) {
        when (fragment) {
            is HomeFragment -> showComponent(showNav = true, showFab = true)
            is MapFragment -> showComponent(showNav = true, showFab = false)
            is Families -> showComponent(showNav = true, showFab = true)
            else -> showComponent(showNav = false, showFab = false)
        }
    }

    private fun showComponent(showNav: Boolean, showFab: Boolean) {
        binding.bottomNavigation.visibility = if (showNav) View.VISIBLE else View.GONE
        binding.fabAddFamily.visibility = if (showFab) View.VISIBLE else View.GONE
    }

    private fun loadFragment(fragment: Fragment, showNav: Boolean, showFab: Boolean, addToBackStack: Boolean = false) {
        showComponent(showNav, showFab)
        
        val transaction = supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
        
        if (addToBackStack) {
            transaction.addToBackStack(null)
        }
        
        transaction.commit()
    }
}