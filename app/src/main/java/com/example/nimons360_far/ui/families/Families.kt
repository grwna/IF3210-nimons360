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
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.nimons360_far.R
import com.example.nimons360_far.backend.models.Family
import com.example.nimons360_far.backend.viewmodels.FamiliesViewModel
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.shape.CircleShape
import android.app.Activity
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

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
    viewModel: FamiliesViewModel = viewModel(
        viewModelStoreOwner = LocalContext.current as AppCompatActivity
    )
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

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

    val listState = rememberLazyListState()
    var displayLimit by remember { mutableIntStateOf(10) }

    LaunchedEffect(filterType, searchQuery) {
        displayLimit = 10
    }

    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItemsCount = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

            totalItemsCount > 0 && lastVisibleItemIndex >= totalItemsCount - 3
        }
    }

    val currentUnpinnedSize by rememberUpdatedState(newValue = unpinnedList.size)

    LaunchedEffect(isAtBottom) {
        if (isAtBottom && displayLimit < currentUnpinnedSize) {
            displayLimit += 10
        }
    }

    val paginatedUnpinnedList = unpinnedList.take(displayLimit)

    LaunchedEffect(Unit) {
        viewModel.fetchFamilies()
    }

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
            state = listState,
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
            } else if (searchedList.isEmpty()) {
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
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(36.dp)
                            )
                        }

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
                if (pinnedList.isNotEmpty()) {
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
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                            shape = shape,
                            color = Color.White
                        ) {
                            Column {
                                FamilyItemRow(family, true, { onNavigateToDetail(family.id) }, { viewModel.togglePin(family.id, true) })
                                if (!isLast) HorizontalDivider(color = DesignBg, thickness = 1.dp)
                            }
                        }
                    }
                }

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
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                            shape = shape,
                            color = Color.White
                        ) {
                            Column {
                                FamilyItemRow(family, false, { onNavigateToDetail(family.id) }, { viewModel.togglePin(family.id, false) })
                                if (!isLast) HorizontalDivider(color = DesignBg, thickness = 1.dp)
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