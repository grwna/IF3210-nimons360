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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.nimons360_far.backend.viewmodels.DetailEvent
import com.example.nimons360_far.backend.viewmodels.FamilyDetailViewModel

// UI Colors Configuration
val LightBlueBg = Color(0xFFEAF2FF)
val RedDanger = Color(0xFFD32F2F)
val YellowWarningBg = Color(0xFFFFF9E6)
val YellowWarningBorder = Color(0xFFFBC02D)

// Fragment - Family Detail - Jetpack Compose UI
class FamilyDetail : Fragment() {
    // Function - On Create View
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Return Compose View - Clean Up When View Destroyed
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    val familyId = arguments?.getInt("familyId") ?: 0
                    FamilyDetailScreen(
                        familyId = familyId,
                        onBack = { requireActivity().supportFragmentManager.popBackStack() }
                    )
                }
            }
        }
    }
}

// Function - Helper Format Created At
fun formatCreatedAt(dateString: String?): String {
    if (dateString.isNullOrEmpty()) {
        return ""
    } else {
        return try {
            val instant = java.time.Instant.parse(dateString)
            val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM yyyy")
                .withZone(java.time.ZoneId.systemDefault())
            " · Created ${formatter.format(instant)}"
        } catch (e: Exception) {
            " · Created $dateString"
        }
    }
}

// Main Composable - Family Detail Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyDetailScreen(
    familyId: Int,
    onBack: () -> Unit,
    viewModel: FamilyDetailViewModel = viewModel()
) {
    // Collect Current Local Context
    val context = LocalContext.current

    // Collect UI States From View Model
    val familyData by viewModel.familyData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val joinErrorMessage by viewModel.joinErrorMessage.collectAsState()

    // Local UI States For Dialog Visibility
    var showJoinDialog by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var joinCodeInput by remember { mutableStateOf("") }

    // Fetch Family Detail On Family ID Changes
    LaunchedEffect(familyId) {
        viewModel.loadFamilyDetail(familyId)
    }

    // Observe View Model Events (Toast & Navigation)
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is DetailEvent.ShowToast -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                is DetailEvent.NavigateBack -> onBack()
            }
        }
    }

    // UI Layout Structure
    Scaffold(
        // Head Bar
        topBar = {
            TopAppBar(
                title = { Text(familyData?.name ?: "Loading...", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = DesignBg
    ) { paddingValues ->
        if (isLoading) {
            // Loading State
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DesignBlue)
            }
        } else {
            // Display Family Data
            familyData?.let { family ->
                LazyColumn(modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)) {
                    // Header : Family Icon & Family Name & Member Count & Created At
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DesignBlue),
                            shape = RoundedCornerShape(0.dp)
                        ) {
                            Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                                // Family Icon
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(
                                            Color.White.copy(alpha = 0.2f),
                                            RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(model = family.iconUrl, contentDescription = null, modifier = Modifier.size(40.dp))
                                }
                                Spacer(modifier = Modifier.width(16.dp))

                                // Family Name & Member Count & Created At
                                Column {
                                    Text(family.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    val memberCount = family.members?.size ?: 0
                                    val memberLabel = if (memberCount <= 1) "member" else "members"
                                    Text(
                                        text = "$memberCount $memberLabel${formatCreatedAt(family.createdAt)}",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 13.sp
                                    )

                                    // Not A Member
                                    if (family.isMember == false) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Surface(
                                            color = Color.White.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Not a member", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Content : Family Code & Family Members & Buttons
                    item {
                        Column(modifier = Modifier.padding(24.dp)) {
                            // Is A Member
                            if (family.isMember == true) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = LightBlueBg),
                                    border = BorderStroke(1.dp, DesignBlue.copy(alpha = 0.1f)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    // Family Code
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("FAMILY CODE", color = DesignBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            // Code
                                            Text(
                                                family.familyCode ?: "XXXXXX",
                                                fontSize = 28.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = DesignBlue,
                                                letterSpacing = 4.sp
                                            )

                                            // Copy Button
                                            IconButton(
                                                onClick = {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    clipboard.setPrimaryClip(ClipData.newPlainText("Code", family.familyCode))
                                                    Toast.makeText(context, "Code copied!", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier
                                                    .background(
                                                        DesignBlue,
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .size(40.dp)
                                            ) {
                                                Icon(Icons.Outlined.ContentCopy, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                            }
                                        }

                                        // Share Note
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Share this code so others can join your family", fontSize = 13.sp, color = DesignTextGrey)
                                    }
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                            }

                            // Family Members
                            Text("MEMBERS", color = DesignTextGrey, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column {
                                    val membersList = family.members ?: emptyList()
                                    membersList.forEachIndexed { index, member ->
                                        MemberItem(
                                            name = member.fullName,
                                            email = member.email,
                                            isJoined = family.isMember == true,
                                            isMe = member.email == viewModel.userEmail
                                        )
                                        if (index < membersList.size - 1) {
                                            HorizontalDivider(color = DesignBg, thickness = 1.dp)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))

                            // Join / Leave Family Button
                            if (family.isMember == true) {
                                // Leave Family Button
                                TextButton(
                                    onClick = { showLeaveDialog = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Leave Family", color = RedDanger, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                                }
                                Spacer(modifier = Modifier.height(32.dp))
                            } else {
                                // Join Family Button & Join Note
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp),
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
                                    onClick = { showJoinDialog = true; viewModel.resetJoinError() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = DesignBlue),
                                    shape = RoundedCornerShape(28.dp)
                                ) {
                                    Text("Join Family", fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                // Need Family Code Note
                                Text(
                                    text = "You'll need the family code to join",
                                    color = DesignTextGrey,
                                    fontSize = 13.sp,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(32.dp))
                            }
                        }
                    }
                }
            }
        }

        // Local Error Triggered
        var localErrorTriggered by remember { mutableStateOf(false) }

        // Dialog - Join Family
        if (showJoinDialog) {
            AlertDialog(
                // Cancel
                onDismissRequest = { showJoinDialog = false },

                // Confirm
                confirmButton = {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Cancel Button
                        TextButton(onClick = { showJoinDialog = false }, modifier = Modifier.weight(1f)) {
                            Text("Cancel", color = DesignTextGrey)
                        }

                        // Join Button
                        Button(
                            onClick = {
                                if (joinCodeInput.length < 6) {
                                    // Error Handling - Invalid Code Length
                                    localErrorTriggered = true
                                } else {
                                    // Try Join Family
                                    viewModel.joinFamily(familyId, joinCodeInput) { showJoinDialog = false }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DesignBlue),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp)
                        ) { Text("Join") }
                    }
                },

                // Dialog Title - Join Family
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier
                            .size(56.dp)
                            .background(LightBlueBg, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.People, null, tint = DesignBlue)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Join Family", fontWeight = FontWeight.Bold)
                    }
                },

                // Dialog Text - Join Family
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Family Code Note
                        Text(
                            "Enter the family code shared by a member of the family to join.",
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        val hasError = joinErrorMessage != null || localErrorTriggered
                        OutlinedTextField(
                            // Family Code Input
                            value = joinCodeInput,
                            onValueChange = {
                                if (it.length <= 6) {
                                    joinCodeInput = it.uppercase()
                                    localErrorTriggered = false
                                    viewModel.resetJoinError()
                                }
                            },
                            label = { Text("Family Code") },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = LocalTextStyle.current.copy(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                letterSpacing = 4.sp,
                                textAlign = TextAlign.Start
                            ),
                            isError = hasError,
                            supportingText = {
                                if (hasError) {
                                    // Display Error
                                    val errorText = if (localErrorTriggered) "Family code must contain 6 characters" else joinErrorMessage
                                    Text(errorText ?: "", color = RedDanger)
                                } else {
                                    // Display Info Note
                                    Text("Ask a member of the family for the code", color = DesignTextGrey)
                                }
                            }
                        )
                    }
                },
                shape = RoundedCornerShape(28.dp),
                containerColor = Color.White
            )
        }

        // Dialog - Leave Family
        if (showLeaveDialog) {
            AlertDialog(
                // Cancel
                onDismissRequest = { showLeaveDialog = false },

                // Confirm
                confirmButton = {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Cancel Button
                        TextButton(onClick = { showLeaveDialog = false }, modifier = Modifier.weight(1f)) {
                            Text("Cancel", color = DesignTextGrey)
                        }

                        // Leave Button
                        Button(
                            onClick = { viewModel.leaveFamily(familyId); showLeaveDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = RedDanger), modifier = Modifier.weight(1f), shape = RoundedCornerShape(24.dp)
                        ) { Text("Leave") }
                    }
                },

                // Dialog Title - Leave Family
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier
                            .size(56.dp)
                            .background(Color(0xFFFFEBEE), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.People, null, tint = RedDanger)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Leave Family", fontWeight = FontWeight.Bold)
                    }
                },

                // Dialog Text - Leave Family
                text = {
                    // Confirmation Text
                    Text(
                        text = buildAnnotatedString {
                            append("Are you sure you want to leave ")
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(familyData?.name ?: "")
                            }
                            append("?")
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                shape = RoundedCornerShape(28.dp), containerColor = Color.White
            )
        }
    }
}

// Reusable Composable - Member Item
@Composable
fun MemberItem(name: String, email: String, isJoined: Boolean, isMe: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isJoined) {
            // Show Member Detail
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(DesignBlue, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))

            // Username & Email
            Column(modifier = Modifier.weight(1f)) {
                // Username
                Text(name, fontWeight = FontWeight.Bold, color = DesignTextBlack, fontSize = 15.sp)

                // Email
                Text(email, color = DesignTextGrey, fontSize = 12.sp)
            }

            // You Tag
            if (isMe) {
                Surface(color = YellowWarningBg, shape = RoundedCornerShape(12.dp)) {
                    Text(
                        "You",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        color = YellowWarningBorder,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            // Hide Member Detail
            val avatarColors = listOf(Color(0xFF1868D8), Color(0xFFD32F2F), Color(0xFF388E3C), Color(0xFFFBC02D), Color(0xFF7B1FA2))
            val safeHash = name.hashCode().let { if (it < 0) it * -1 else it }
            val blurColor = avatarColors[safeHash % avatarColors.size]

            // User Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .blur(radius = 12.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    .background(blurColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.6f))
            }
            Spacer(modifier = Modifier.width(16.dp))

            // Username & Email
            Column(modifier = Modifier.weight(1f)) {
                // Username
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .height(14.dp)
                        .background(Color.LightGray.copy(alpha = 0.8f), RoundedCornerShape(7.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Email
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(12.dp)
                        .background(Color.LightGray.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                )
            }
        }
    }
}