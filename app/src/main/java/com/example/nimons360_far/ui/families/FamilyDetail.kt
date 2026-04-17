package com.example.nimons360_far.ui.families

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import coil.compose.AsyncImage
import com.example.nimons360_far.api.FamilyAPI
import com.example.nimons360_far.model.Family
import com.example.nimons360_far.model.JoinFamilyRequest
import com.example.nimons360_far.model.LeaveFamilyRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

val LightBlueBg = Color(0xFFEAF2FF)
val RedDanger = Color(0xFFD32F2F)
val YellowWarningBg = Color(0xFFFFF9E6)
val YellowWarningBorder = Color(0xFFFBC02D)

class FamilyDetail : Fragment() {

    private val api: FamilyAPI by lazy {
        Retrofit.Builder()
            .baseUrl("https://mad.labpro.hmif.dev")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FamilyAPI::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    val familyId = arguments?.getInt("familyId") ?: 0
                    FamilyDetailScreen(
                        api = api,
                        familyId = familyId,
                        onBack = { requireActivity().supportFragmentManager.popBackStack() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyDetailScreen(
    api: FamilyAPI,
    familyId: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var familyData by remember { mutableStateOf<Family?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var joinCodeInput by remember { mutableStateOf("") }
    var joinErrorMessage by remember { mutableStateOf<String?>(null) }
    val sharedPreferences = context.getSharedPreferences("Nimons360Prefs", Context.MODE_PRIVATE)
    val token = "Bearer ${sharedPreferences.getString("AUTH_TOKEN", "")}"

    fun loadFamilyDetail() {
        if (familyId == 0) return
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val response = api.getFamilyDetail(token, familyId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        familyData = response.body()?.data
                    } else {
                        Toast.makeText(context, "Failed to load family data", Toast.LENGTH_SHORT).show()
                    }
                    isLoading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    Toast.makeText(context, "Connection error", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(key1 = familyId) {
        loadFamilyDetail()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(familyData?.name ?: "Loading...", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = DesignBg
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DesignBlue)
            }
        } else {
            familyData?.let { family ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(paddingValues)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DesignBlue),
                        shape = RoundedCornerShape(0.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = family.iconUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(family.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    "${family.members?.size ?: 0} members",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.padding(24.dp)) {
                        if (family.isMember == true) {
                            Text("FAMILY CODE", color = DesignBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = LightBlueBg),
                                border = BorderStroke(1.dp, DesignBlue.copy(alpha = 0.1f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        family.familyCode ?: "XXXXXX",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DesignBlue,
                                        letterSpacing = 4.sp
                                    )
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Code", family.familyCode))
                                            Toast.makeText(context, "Code copied!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.background(DesignBlue, RoundedCornerShape(8.dp))
                                    ) {
                                        Icon(Icons.Default.Share, null, tint = Color.White)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        Text("MEMBERS", color = DesignTextGrey, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(family.members ?: emptyList()) { member ->
                                val userEmail = sharedPreferences.getString("USER_EMAIL", "") ?: ""
                                MemberItem(member.fullName, member.email, family.isMember == true, member.email == userEmail)
                            }
                        }

                        if (family.isMember == true) {
                            TextButton(
                                onClick = { showLeaveDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Leave Family", color = RedDanger, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                colors = CardDefaults.cardColors(containerColor = YellowWarningBg),
                                border = BorderStroke(1.dp, YellowWarningBorder)
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Info, null, tint = YellowWarningBorder, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Join this family to see member details and interact with them on the map.", fontSize = 12.sp, color = DesignTextBlack)
                                }
                            }
                            Button(
                                onClick = { showJoinDialog = true },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DesignBlue),
                                shape = RoundedCornerShape(28.dp)
                            ) {
                                Text("Join Family", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        if (showJoinDialog) {
            AlertDialog(
                onDismissRequest = { showJoinDialog = false },
                confirmButton = {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { showJoinDialog = false }, modifier = Modifier.weight(1f)) {
                            Text("Cancel", color = DesignTextGrey)
                        }
                        Button(
                            onClick = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        val res = api.joinFamily(token, JoinFamilyRequest(familyId, joinCodeInput))
                                        withContext(Dispatchers.Main) {
                                            if (res.isSuccessful) {
                                                showJoinDialog = false
                                                loadFamilyDetail()
                                            } else {
                                                joinErrorMessage = "Wrong Family Code!"
                                            }
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) { joinErrorMessage = "Error connection" }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DesignBlue),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp)
                        ) { Text("Join") }
                    }
                },
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.size(56.dp).background(LightBlueBg, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, null, tint = DesignBlue)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Join Family", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column {
                        Text("Enter the family code shared by a member of the family to join.", textAlign = TextAlign.Center, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = joinCodeInput,
                            onValueChange = { if (it.length <= 6) joinCodeInput = it.uppercase() },
                            label = { Text("Family Code") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = joinErrorMessage != null,
                            supportingText = { if (joinErrorMessage != null) Text(joinErrorMessage!!, color = RedDanger) }
                        )
                    }
                },
                shape = RoundedCornerShape(28.dp),
                containerColor = Color.White
            )
        }

        if (showLeaveDialog) {
            AlertDialog(
                onDismissRequest = { showLeaveDialog = false },
                confirmButton = {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { showLeaveDialog = false }, modifier = Modifier.weight(1f)) {
                            Text("Cancel", color = DesignTextGrey)
                        }
                        Button(
                            onClick = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        val res = api.leaveFamily(token, LeaveFamilyRequest(familyId))
                                        withContext(Dispatchers.Main) {
                                            if (res.isSuccessful) onBack()
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RedDanger),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp)
                        ) { Text("Leave") }
                    }
                },
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.size(56.dp).background(Color(0xFFFFEBEE), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Close, null, tint = RedDanger)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Leave Family", fontWeight = FontWeight.Bold)
                    }
                },
                text = { Text("Are you sure you want to leave ${familyData?.name}?", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                shape = RoundedCornerShape(28.dp),
                containerColor = Color.White
            )
        }
    }
}

@Composable
fun MemberItem(name: String, email: String, isJoined: Boolean, isMe: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(DesignBg, CircleShape).then(if (!isJoined) Modifier.blur(8.dp) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Text(name.take(1), fontWeight = FontWeight.Bold, color = DesignBlue)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f).then(if (!isJoined) Modifier.blur(4.dp) else Modifier)) {
            Text(name, fontWeight = FontWeight.Bold, color = DesignTextBlack, fontSize = 15.sp)
            Text(email, color = DesignTextGrey, fontSize = 12.sp)
        }
        if (isJoined && isMe) {
            Surface(color = YellowWarningBg, shape = RoundedCornerShape(12.dp)) {
                Text("You", modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp), fontSize = 11.sp, color = YellowWarningBorder, fontWeight = FontWeight.Bold)
            }
        }
    }
    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
}