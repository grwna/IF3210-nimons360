package com.example.nimons360_far.ui.families

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.nimons360_far.R
import com.example.nimons360_far.backend.models.Family
import com.example.nimons360_far.backend.viewmodels.FamiliesViewModel

val DesignBg = Color(0xFFF8F9FA)
val DesignBlue = Color(0xFF1868D8)
val DesignTextBlack = Color(0xFF222222)
val DesignTextGrey = Color(0xFF666666)
val DesignSearchBg = Color(0xFFFFFFFF)
val DesignFabBg = Color(0xFFEAF2FF)

class Families : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    FamiliesScreen(
                        onNavigateToCreate = {
                            requireActivity().supportFragmentManager.beginTransaction()
                                .replace(R.id.fragment_container, CreateFamily())
                                .addToBackStack(null)
                                .commit()
                        },
                        onNavigateToDetail = { familyId ->
                            val familyDetail = FamilyDetail().apply {
                                arguments = Bundle().apply { putInt("familyId", familyId) }
                            }
                            requireActivity().supportFragmentManager.beginTransaction()
                                .replace(R.id.fragment_container, familyDetail)
                                .addToBackStack(null)
                                .commit()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamiliesScreen(
    onNavigateToCreate: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    viewModel: FamiliesViewModel = viewModel()
) {
    val allFamilies by viewModel.allFamilies.collectAsState()
    val myFamilies by viewModel.myFamilies.collectAsState()
    val pinnedIds by viewModel.pinnedIds.collectAsState()
    val filterType by viewModel.filterType.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val baseList = if (filterType == "All") allFamilies else myFamilies
    val searchedList = baseList.filter { it.name.contains(searchQuery, ignoreCase = true) }
    val pinnedList = searchedList.filter { pinnedIds.contains(it.id) }
    val unpinnedList = searchedList.filter { !pinnedIds.contains(it.id) }

    Scaffold(
        topBar = {
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Families",
                    color = DesignTextBlack,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(start = 24.dp, top = 20.dp, end = 24.dp, bottom = 16.dp)
                )
            }
        },
        containerColor = DesignBg
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(DesignBg)
                .padding(paddingValues)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    placeholder = { Text("Search families...", color = DesignTextGrey) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = DesignTextGrey) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DesignSearchBg,
                        unfocusedContainerColor = DesignSearchBg,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(24.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.padding(horizontal = 24.dp)) {
                    CustomFilterChip("All", filterType == "All") { viewModel.setFilterType("All") }
                    Spacer(modifier = Modifier.width(12.dp))
                    CustomFilterChip("My Families", filterType == "My Families") { viewModel.setFilterType("My Families") }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(50.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = DesignBlue)
                    }
                }
            } else {
                if (pinnedList.isNotEmpty()) {
                    item { SectionHeader("PINNED") }
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column {
                                pinnedList.forEachIndexed { index, family ->
                                    FamilyItemRow(family, true, { onNavigateToDetail(family.id) }, { viewModel.togglePin(family.id, true) })
                                    if (index < pinnedList.size - 1) {
                                        HorizontalDivider(color = DesignBg, thickness = 1.dp)
                                    }
                                }
                            }
                        }
                    }
                }

                if (unpinnedList.isNotEmpty()) {
                    item { SectionHeader("ALL FAMILIES") }
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column {
                                unpinnedList.forEachIndexed { index, family ->
                                    FamilyItemRow(family, false, { onNavigateToDetail(family.id) }, { viewModel.togglePin(family.id, false) })
                                    if (index < unpinnedList.size - 1) {
                                        HorizontalDivider(color = DesignBg, thickness = 1.dp)
                                    }
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text = text,
        color = DesignTextGrey,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 12.dp)
    )
}

@Composable
fun CustomFilterChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (isSelected) DesignBlue else Color.White,
        shape = RoundedCornerShape(8.dp),
        border = if (isSelected) null else BorderStroke(1.dp, Color.LightGray)
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else DesignTextBlack,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun FamilyItemRow(family: Family, isPinned: Boolean, onClick: () -> Unit, onPinClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier.size(48.dp).background(DesignBg, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(model = family.iconUrl, contentDescription = null, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(family.name, color = DesignTextBlack, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        IconButton(onClick = onPinClick) {
            Icon(
                imageVector = if (isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                contentDescription = "Pin",
                tint = if (isPinned) DesignBlue else Color.LightGray,
                modifier = Modifier.size(20.dp).rotate(45f)
            )
        }
    }
}