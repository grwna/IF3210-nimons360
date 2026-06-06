package com.example.nimons360_far.ui.families

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.shape.CircleShape
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.nimons360_far.R
import com.example.nimons360_far.backend.models.Family
import com.example.nimons360_far.backend.utils.Refreshable
import com.example.nimons360_far.ui.viewmodels.FamiliesViewModel
import com.example.nimons360_far.utils.AppColors.DesignBg
import com.example.nimons360_far.utils.AppColors.DesignBlue
import com.example.nimons360_far.utils.AppColors.DesignTextBlack
import com.example.nimons360_far.utils.AppColors.DesignTextGrey
import com.example.nimons360_far.utils.AppColors.DesignSearchBg

// Fragment - Families - Jetpack Compose UI & Implements Refreshable
class Families : Fragment(), Refreshable {
    // Initialize View Model
    private lateinit var viewModel: FamiliesViewModel

    // Function - On Create - Share View Model With Activity (Persistent State)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[FamiliesViewModel::class.java]
    }

    // Function - On Create View
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Return Compose View - Clean Up When View Destroyed
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    FamiliesScreen(
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

    // Function - On Refresh
    override fun onRefresh() {
        viewModel.fetchFamilies()
    }
}

// Main Composable - Families Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamiliesScreen(
    onNavigateToDetail: (Int) -> Unit,
    viewModel: FamiliesViewModel = viewModel(
        viewModelStoreOwner = LocalContext.current as AppCompatActivity
    )
) {
    // Collect UI States From View Model
    val allFamilies by viewModel.allFamilies.collectAsState()
    val myFamilies by viewModel.myFamilies.collectAsState()
    val pinnedIds by viewModel.pinnedIds.collectAsState()
    val filterType by viewModel.filterType.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Filter & Search Logic
    val baseList = if (filterType == "All") allFamilies else myFamilies
    val searchedList = baseList.filter { it.name.contains(searchQuery, ignoreCase = true) }
    val pinnedList = searchedList.filter { pinnedIds.contains(it.id) }
    val unpinnedList = searchedList.filter { !pinnedIds.contains(it.id) }

    // Pagination / Infinite Scroll Logic
    val listState = rememberLazyListState()
    var displayLimit by remember { mutableIntStateOf(10) }

    // Reset Pagination Limit On Filter / Search Changes
    LaunchedEffect(filterType, searchQuery) {
        displayLimit = 10
    }

    // Check If User At The Bottom List
    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItemsCount = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItemsCount > 0 && lastVisibleItemIndex >= totalItemsCount - 3
        }
    }

    // Load More Items
    val currentUnpinnedSize by rememberUpdatedState(newValue = unpinnedList.size)
    LaunchedEffect(isAtBottom) {
        if (isAtBottom && displayLimit < currentUnpinnedSize) {
            displayLimit += 10
        }
    }
    val paginatedUnpinnedList = unpinnedList.take(displayLimit)

    // Initial Data Fetch
    LaunchedEffect(Unit) {
        viewModel.fetchFamilies()
    }

    // UI Layout Structure
    Scaffold(
        // Head Bar
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
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(DesignBg)
                .padding(paddingValues)
        ) {
            item {
                // Search Families
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

                // Filter Families
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.padding(horizontal = 24.dp)) {
                    CustomFilterChip("All", filterType == "All") { viewModel.setFilterType("All") }
                    Spacer(modifier = Modifier.width(12.dp))
                    CustomFilterChip("My Families", filterType == "My Families") { viewModel.setFilterType("My Families") }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (isLoading) {
                // Loading State
                item {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .padding(50.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = DesignBlue)
                    }
                }
            } else if (searchedList.isEmpty()) {
                // Empty Data State - Search
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 80.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(Color(0xFFFFEBEE), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Empty",
                                tint = com.example.nimons360_far.utils.AppColors.RedDanger,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Empty Message
                        Spacer(modifier = Modifier.height(16.dp))
                        val emptyMessage = if (searchQuery.isNotEmpty()) {
                            "No families found for '$searchQuery'"
                        } else if (filterType == "My Families") {
                            "You haven't joined any families"
                        } else {
                            "No families available"
                        }
                        Text(
                            text = emptyMessage,
                            color = DesignTextBlack,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (searchQuery.isEmpty() && filterType == "My Families") {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Create a new family or join an existing one.",
                                color = DesignTextGrey,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            } else {
                // Not Empty Data State - Pinned
                if (pinnedList.isNotEmpty()) {
                    // Render Pinned Families
                    item { SectionHeader("PINNED") }
                    itemsIndexed(pinnedList) { index, family ->
                        val isFirst = index == 0
                        val isLast = index == pinnedList.lastIndex
                        val shape = when {
                            isFirst && isLast -> RoundedCornerShape(16.dp)
                            isFirst -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                            isLast -> RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                            else -> RectangleShape
                        }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            shape = shape,
                            color = Color.White
                        ) {
                            Column {
                                FamilyItemRow(family, true, { onNavigateToDetail(family.id) }, { viewModel.togglePin(family.id, true) })
                                if (!isLast) {
                                    HorizontalDivider(color = DesignBg, thickness = 1.dp)
                                }
                            }
                        }
                    }
                }

                // Render All Unpinned Families
                if (paginatedUnpinnedList.isNotEmpty()) {
                    item { SectionHeader("ALL FAMILIES") }
                    itemsIndexed(paginatedUnpinnedList) { index, family ->
                        val isFirst = index == 0
                        val isLast = index == paginatedUnpinnedList.lastIndex
                        val shape = when {
                            isFirst && isLast -> RoundedCornerShape(16.dp)
                            isFirst -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                            isLast -> RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                            else -> RectangleShape
                        }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            shape = shape,
                            color = Color.White
                        ) {
                            Column {
                                FamilyItemRow(family, false, { onNavigateToDetail(family.id) }, { viewModel.togglePin(family.id, false) })
                                if (!isLast) {
                                    HorizontalDivider(color = DesignBg, thickness = 1.dp)
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

// Reusable Composable - Section Header
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

// Reusable Composable - Custom Filter Chip
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

// Reusable Composable - Family Item Row
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
                modifier = Modifier
                    .size(48.dp)
                    .background(DesignBg, RoundedCornerShape(8.dp)),
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
                modifier = Modifier
                    .size(20.dp)
                    .rotate(45f)
            )
        }
    }
}