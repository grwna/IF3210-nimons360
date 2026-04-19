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
import com.example.nimons360_far.ui.viewmodels.DetailEvent
import com.example.nimons360_far.ui.viewmodels.FamilyDetailViewModel

val LightBlueBg = Color(0xFFEAF2FF)
val RedDanger = Color(0xFFD32F2F)
val YellowWarningBg = Color(0xFFFFF9E6)
val YellowWarningBorder = Color(0xFFFBC02D)

class FamilyDetail : Fragment() {
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
                        familyId = familyId,
                        onBack = { requireActivity().supportFragmentManager.popBackStack() }
                    )
                }
            }
        }
    }
}

fun formatCreatedAt(dateString: String?): String {
    if (dateString.isNullOrEmpty()) return ""
    return try {
        val instant = java.time.Instant.parse(dateString)
        val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM yyyy")
            .withZone(java.time.ZoneId.systemDefault())
        " · Created ${formatter.format(instant)}"
    } catch (e: Exception) {
        " · Created $dateString"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyDetailScreen(
    familyId: Int,
    onBack: () -> Unit,
    viewModel: FamilyDetailViewModel = viewModel()
) {
    val context = LocalContext.current
    val familyData by viewModel.familyData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val joinErrorMessage by viewModel.joinErrorMessage.collectAsState()

    var showJoinDialog by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var joinCodeInput by remember { mutableStateOf("") }

    LaunchedEffect(familyId) {
        viewModel.loadFamilyDetail(familyId)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is DetailEvent.ShowToast -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                is DetailEvent.NavigateBack -> onBack()
            }
        }
    }

    Scaffold(
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
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DesignBlue)
            }
        } else {
            familyData?.let { family ->
                LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DesignBlue),
                            shape = RoundedCornerShape(0.dp)
                        ) {
                            Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(64.dp).background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(model = family.iconUrl, contentDescription = null, modifier = Modifier.size(40.dp))
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(family.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    val memberCount = family.members?.size ?: 0
                                    val memberLabel = if (memberCount <= 1) "member" else "members"

                                    Text(
                                        text = "$memberCount $memberLabel${formatCreatedAt(family.createdAt)}",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 13.sp
                                    )
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

                    item {
                        Column(modifier = Modifier.padding(24.dp)) {
                            if (family.isMember == true) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = LightBlueBg),
                                    border = BorderStroke(1.dp, DesignBlue.copy(alpha = 0.1f)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("FAMILY CODE", color = DesignBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
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
                                                modifier = Modifier.background(DesignBlue, RoundedCornerShape(8.dp)).size(40.dp)
                                            ) {
                                                Icon(Icons.Outlined.ContentCopy, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Share this code so others can join your family", fontSize = 13.sp, color = DesignTextGrey)
                                    }
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                            }

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

                            if (family.isMember == true) {
                                TextButton(
                                    onClick = { showLeaveDialog = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Leave Family", color = RedDanger, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                                }
                                Spacer(modifier = Modifier.height(32.dp))
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
                                    onClick = { showJoinDialog = true; viewModel.resetJoinError() },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = DesignBlue),
                                    shape = RoundedCornerShape(28.dp)
                                ) {
                                    Text("Join Family", fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
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

        var localErrorTriggered by remember { mutableStateOf(false) }

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
                                if (joinCodeInput.length < 6) {
                                    localErrorTriggered = true
                                } else {
                                    viewModel.joinFamily(familyId, joinCodeInput) { showJoinDialog = false }
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
                            Icon(Icons.Default.People, null, tint = DesignBlue)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Join Family", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Enter the family code shared by a member of the family to join.",
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        val hasError = joinErrorMessage != null || localErrorTriggered

                        OutlinedTextField(
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
                                    val errorText = if (localErrorTriggered) "Family code must contain 6 characters" else joinErrorMessage
                                    Text(errorText ?: "", color = RedDanger)
                                } else {
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

        if (showLeaveDialog) {
            AlertDialog(
                onDismissRequest = { showLeaveDialog = false },
                confirmButton = {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { showLeaveDialog = false }, modifier = Modifier.weight(1f)) { Text("Cancel", color = DesignTextGrey) }
                        Button(
                            onClick = { viewModel.leaveFamily(familyId); showLeaveDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = RedDanger), modifier = Modifier.weight(1f), shape = RoundedCornerShape(24.dp)
                        ) { Text("Leave") }
                    }
                },
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.size(56.dp).background(Color(0xFFFFEBEE), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.People, null, tint = RedDanger)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Leave Family", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
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

@Composable
fun MemberItem(name: String, email: String, isJoined: Boolean, isMe: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isJoined) {
            Box(
                modifier = Modifier.size(40.dp).background(DesignBlue, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Bold, color = DesignTextBlack, fontSize = 15.sp)
                Text(email, color = DesignTextGrey, fontSize = 12.sp)
            }

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
            val avatarColors = listOf(Color(0xFF1868D8), Color(0xFFD32F2F), Color(0xFF388E3C), Color(0xFFFBC02D), Color(0xFF7B1FA2))
            val safeHash = name.hashCode().let { if (it < 0) it * -1 else it }
            val blurColor = avatarColors[safeHash % avatarColors.size]

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

            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .height(14.dp)
                        .background(Color.LightGray.copy(alpha = 0.8f), RoundedCornerShape(7.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
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