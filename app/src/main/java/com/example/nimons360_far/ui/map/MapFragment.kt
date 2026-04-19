package com.example.nimons360_far.ui.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.example.nimons360_far.ui.main.MainActivity

class MapFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val mainActivity = requireActivity() as MainActivity

                val userGeoPoint by mainActivity.userGeoPoint.collectAsState()
                val currentHeading by mainActivity.currentHeading.collectAsState()
                val familyMembers by mainActivity.familyMembers.collectAsState()

                MapScreen(
                    userGeoPoint = userGeoPoint,
                    currentHeading = currentHeading,
                    familyMembers = familyMembers
                )
            }
        }
    }
}
