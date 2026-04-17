package com.example.nimons360_far.ui.families

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import coil.compose.AsyncImage
import com.example.nimons360_far.R
import com.example.nimons360_far.api.FamilyAPI
import com.example.nimons360_far.data.AppDatabase
import com.example.nimons360_far.data.PinnedFamily
import com.example.nimons360_far.model.Family
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

val DesignBg = Color(0xFFF8F9FA)
val DesignBlue = Color(0xFF1868D8)
val DesignTextBlack = Color(0xFF222222)
val DesignTextGrey = Color(0xFF666666)
val DesignSearchBg = Color(0xFFEEEFF2)
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
                                arguments = Bundle().apply {
                                    putInt("familyId", familyId)
                                }
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
    onNavigateToDetail: (Int) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val api = remember {
        Retrofit.Builder()
            .baseUrl("https://mad.labpro.hmif.dev")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FamilyAPI::class.java)
    }

    val database = remember { AppDatabase.getDatabase(context) }
    val pinnedDao = database.pinnedFamilyDao()
    val sharedPreferences = context.getSharedPreferences("Nimons360Prefs", Context.MODE_PRIVATE)
    val token = sharedPreferences.getString("AUTH_TOKEN", "") ?: ""
    val bearerToken = "Bearer $token"
    var allFamilies by remember { mutableStateOf<List<Family>>(emptyList()) }
    var myFamilies by remember { mutableStateOf<List<Family>>(emptyList()) }
    var pinnedIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var filterType by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                if (token.isNotEmpty()) {
                    val allRes = api.getAllFamilies(bearerToken)
                    val myRes = api.getMyFamilies(bearerToken)

                    withContext(Dispatchers.Main) {
                        if (allRes.isSuccessful) allFamilies = allRes.body()?.data ?: emptyList()
                        if (myRes.isSuccessful) myFamilies = myRes.body()?.data ?: emptyList()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to load families", Toast.LENGTH_SHORT).show()
                }
            } finally {
                withContext(Dispatchers.Main) { isLoading = false }
            }
        }
    }

    LaunchedEffect(Unit) {
        pinnedDao.getAllPinned().collect { pinnedList ->
            pinnedIds = pinnedList.map { it.familyId }.toSet()
        }
    }

    val baseList = if (filterType == "All") allFamilies else myFamilies
    val searchedList = baseList.filter { it.name.contains(searchQuery, ignoreCase = true) }
    val pinnedList = searchedList.filter { pinnedIds.contains(it.id) }
    val unpinnedList = searchedList.filter { !pinnedIds.contains(it.id) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreate,
                containerColor = DesignFabBg,
                contentColor = DesignBlue,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Create")
            }
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            Text(
                text = "Families",
                color = DesignTextBlack,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 16.dp)
            )

            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
                placeholder = { Text("Search families...", color = DesignTextGrey) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = DesignTextGrey) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = DesignSearchBg,
                    unfocusedContainerColor = DesignSearchBg,
                    cursorColor = DesignBlue,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = DesignTextBlack,
                    unfocusedTextColor = DesignTextBlack
                ),
                shape = RoundedCornerShape(24.dp)
            )

            Row(modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp)) {
                CustomFilterChip(
                    label = "All",
                    isSelected = filterType == "All",
                    onClick = { filterType = "All" }
                )
                Spacer(modifier = Modifier.width(12.dp))
                CustomFilterChip(
                    label = "My Families",
                    isSelected = filterType == "My Families",
                    onClick = { filterType = "My Families" }
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = DesignBlue)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().background(DesignBg)) {
                    if (pinnedList.isNotEmpty()) {
                        item { SectionHeader("PINNED") }
                        items(pinnedList) { family ->
                            FamilyItemCard(
                                family = family,
                                isPinned = true,
                                onClick = { onNavigateToDetail(family.id) },
                                onPinClick = {
                                    coroutineScope.launch(Dispatchers.IO) { pinnedDao.unpinFamily(family.id) }
                                }
                            )
                        }
                    }

                    if (unpinnedList.isNotEmpty()) {
                        item { SectionHeader("ALL FAMILIES") }
                        items(unpinnedList) { family ->
                            FamilyItemCard(
                                family = family,
                                isPinned = false,
                                onClick = { onNavigateToDetail(family.id) },
                                onPinClick = {
                                    coroutineScope.launch(Dispatchers.IO) { pinnedDao.pinFamily(PinnedFamily(family.id)) }
                                }
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
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
fun FamilyItemCard(
    family: Family,
    isPinned: Boolean,
    onClick: () -> Unit,
    onPinClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(0.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .fillMaxWidth(),
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
                        AsyncImage(
                            model = family.iconUrl,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = family.name,
                        color = DesignTextBlack,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                IconButton(onClick = onPinClick) {
                    Icon(
                        imageVector = if (isPinned) Icons.Default.Star else Icons.Outlined.Star,
                        contentDescription = "Pin",
                        tint = if (isPinned) DesignBlue else Color.LightGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            HorizontalDivider(
                color = DesignBg,
                thickness = 1.dp,
                modifier = Modifier.padding(start = 80.dp, end = 0.dp)
            )
        }
    }
}