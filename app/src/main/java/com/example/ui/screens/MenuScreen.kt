package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppTheme
import com.example.viewmodel.TimetableViewModel

@Composable
fun MenuScreen(
    viewModel: TimetableViewModel,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val context = LocalContext.current
    val timetableData by viewModel.timetableData.collectAsState()
    val isLiveSource by viewModel.isLiveSource.collectAsState()
    val lastCheckedTime by viewModel.lastCheckedTime.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val themeOverride by viewModel.themeOverride.collectAsState()
    val versionTapCount by viewModel.versionTapCount.collectAsState()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = "Menu & Settings",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = colors.text
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            // 1. GENERAL SETTINGS CARD
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.line),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Lecture Reminders
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.showNotificationSettings(true) }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Notifications, contentDescription = null, tint = colors.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Lecture Reminders",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.text
                                )
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = colors.muted)
                        }

                        HorizontalDivider(color = colors.line, thickness = 0.5.dp)

                        // Appearance Setting
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showThemeDialog = true }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Palette, contentDescription = null, tint = colors.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Appearance",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.text
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = themeOverride.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.muted
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = colors.muted)
                            }
                        }
                    }
                }
            }

            // 2. TIMETABLE SYNCHRONIZATION CARD
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.line),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Timetable Sync Status",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.text
                                )
                                Text(
                                    text = if (isLiveSource) "Online (Live sync active)" else "Bundled copy (Fall 2026)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.muted
                                )
                            }

                            Button(
                                onClick = { viewModel.refreshTimetable() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.primarySoft,
                                    contentColor = colors.accentText
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("menu_refresh_button")
                            ) {
                                if (isRefreshing) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = colors.primary, strokeWidth = 2.dp)
                                } else {
                                    Text("Update", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Last checked: $lastCheckedTime",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.muted
                        )
                    }
                }
            }

            // 3. OFFICIAL CHANNELS & ROLE CHANGE
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.line),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // WhatsApp Channel Link
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://whatsapp.com/channel/0029VaJwzEZ11ulHnliDcu1e"))
                                    context.startActivity(intent)
                                }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Campaign, contentDescription = null, tint = colors.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Official Updates (WhatsApp)",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.text
                                )
                            }
                            Icon(Icons.Default.OpenInNew, contentDescription = null, tint = colors.muted, modifier = Modifier.size(16.dp))
                        }

                        HorizontalDivider(color = colors.line, thickness = 0.5.dp)

                        // Role Change / Wizard Reset
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showResetConfirm = true }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.SupervisorAccount, contentDescription = null, tint = colors.danger)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Switch Role or Profile",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.danger
                                )
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = colors.muted)
                        }
                    }
                }
            }

            // 4. ABOUT & FOOTER SECTION
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colors.surface2,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Class Flow",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.text,
                            modifier = Modifier
                                .clickable { viewModel.onVersionTapped() }
                                .testTag("menu_app_name_text")
                        )
                        Text(
                            text = "Thal University Bhakkar - Unofficial App\nDeveloped to serve DCS&IT department and student body.\nVersion 1.6 - Unofficial",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.muted,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choose Appearance Theme", fontWeight = FontWeight.Bold, color = colors.text) },
            text = {
                Column {
                    listOf("system", "light", "dark").forEach { theme ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.saveThemeOverride(theme)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = theme.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.text
                            )
                            if (themeOverride == theme) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = colors.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = colors.surface
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Switch Role?", fontWeight = FontWeight.Bold, color = colors.text) },
            text = { Text("Are you sure you want to reset your profile and switch roles?", color = colors.text) },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirm = false
                        viewModel.clearProfileAndShowWizard()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.danger)
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel", color = colors.muted)
                }
            },
            containerColor = colors.surface
        )
    }
}
