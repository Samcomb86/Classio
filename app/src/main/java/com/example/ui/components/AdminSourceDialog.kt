package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.config.AppConfig
import com.example.ui.theme.AppTheme

@Composable
fun AdminSourceDialog(
    initialUrl: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val colors = AppTheme.colors
    var urlInput by remember { mutableStateOf(initialUrl) }
    val convertedUrl = remember(urlInput) { AppConfig.normalizeSourceUrl(urlInput) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("admin_source_dialog"),
            shape = RoundedCornerShape(16.dp),
            color = colors.surface,
            border = BorderStroke(1.dp, colors.line),
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = colors.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Admin Source Link",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.text
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.muted)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Paste a new Google Sheet or Google Drive link when the university publishes an updated timetable.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("University Sheet or Drive Link", color = colors.muted) },
                    placeholder = { Text("https://docs.google.com/spreadsheets/d/...", color = colors.muted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_url_input"),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = false,
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.line,
                        focusedTextColor = colors.text,
                        unfocusedTextColor = colors.text
                    )
                )

                if (convertedUrl.isNotBlank() && convertedUrl != urlInput) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = colors.surface2,
                        border = BorderStroke(1.dp, colors.line),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Auto-converted Download URL:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.primary
                            )
                            Text(
                                text = convertedUrl,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.muted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        urlInput = ""
                        onSave("")
                    }) {
                        Text("Reset to Default", color = colors.muted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(urlInput) },
                        modifier = Modifier.testTag("save_admin_url_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = colors.onPrimary
                        )
                    ) {
                        Text("Save & Sync")
                    }
                }
            }
        }
    }
}
