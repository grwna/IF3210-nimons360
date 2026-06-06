package com.example.nimons360_far.ui.families

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nimons360_far.R
import com.example.nimons360_far.utils.AppColors.DesignBlue
import com.example.nimons360_far.utils.AppColors.DesignTextBlack
import com.example.nimons360_far.utils.AppColors.DesignTextGrey
import com.example.nimons360_far.utils.AppColors.CancelTextDark

// Composable - Broadcast Message Bottom Sheet
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BroadcastBottomSheet(onDismiss: () -> Unit, onSend: (String) -> Unit) 
{
    var messageText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 40.dp)
        ) {
            // Title
            Text(
                text = "Send Message",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = DesignTextBlack
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Outline Input
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                label = { Text("Message") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    if (messageText.isNotEmpty()) {
                        IconButton(onClick = { messageText = "" }) {
                            Icon(
                                painter = painterResource(id = R.drawable.x_clear),
                                contentDescription = "Clear Message",
                                tint = Color.Unspecified
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DesignBlue,
                    focusedLabelColor = DesignBlue,
                    cursorColor = DesignBlue
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Label
            Text(
                text = "This message will be sent to your family member",
                fontSize = 12.sp,
                color = DesignTextGrey
            )
            Spacer(modifier = Modifier.height(32.dp))

            // Action Sections Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismiss,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Cancel",
                        color = CancelTextDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = {
                        if (messageText.isNotEmpty()) {
                            onSend(messageText)
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DesignBlue),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier .height(56.dp) .width(180.dp),
                    enabled = messageText.isNotEmpty()
                ) {
                    Text(
                        text = "Send",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
