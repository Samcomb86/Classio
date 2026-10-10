package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.notifications.NotificationSettings
import com.example.ui.theme.AppTheme
import com.example.viewmodel.TimetableViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    viewModel: TimetableViewModel,
    onBack: () -> Unit
) {
    val colors = AppTheme.colors
    val context = LocalContext.current
    val settings by viewModel.notificationSettings.collectAsState()

    var showLeadTimeMenu by remember { mutableStateOf(false) }
    var showTimePickerModal by remember { mutableStateOf(false) }
    var snackbarHostState = remember { SnackbarHostState() }
    var testNotifSentMsg by remember { mutableStateOf<String?>(null) }

    val permissionStatus = remember(settings.hasAskedPermission) {
        checkPermissionStatus(context, settings.hasAskedPermission)
    }

    LaunchedEffect(testNotifSentMsg) {
        testNotifSentMsg?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            testNotifSentMsg = null
        }
    }

    BackHandler {
        onBack()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = colors.bg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Notification Settings",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.text
                        )
                        Text(
                            text = "Lecture reminders and alerts",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.muted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("notif_settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.text
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.bg
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(2.dp)) }

            // 1. PERMISSION STATUS CARD
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = colors.surface2,
                    border = BorderStroke(1.dp, colors.line)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Permission Status",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.text
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = permissionStatus,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = when (permissionStatus) {
                                    "Allowed" -> colors.success
                                    "Blocked in phone settings" -> colors.danger
                                    else -> colors.muted
                                },
                                modifier = Modifier.testTag("permission_status_text")
                            )
                        }

                        if (permissionStatus == "Blocked in phone settings") {
                            TextButton(
                                onClick = { openAppSettings(context) },
                                modifier = Modifier.testTag("open_app_settings_button")
                            ) {
                                Text(
                                    text = "Open settings",
                                    fontWeight = FontWeight.Bold,
                                    color = colors.primary
                                )
                            }
                        }
                    }
                }
            }

            // 2. MASTER SWITCH
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.line)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Lecture Reminders",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.text
                                )
                                Text(
                                    text = "Get notified before each lecture starts",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.muted
                                )
                            }
                            Switch(
                                checked = settings.enabled,
                                onCheckedChange = { isChecked ->
                                    viewModel.updateNotificationSettings(settings.copy(enabled = isChecked))
                                },
                                modifier = Modifier.testTag("notif_master_switch"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = colors.onPrimary,
                                    checkedTrackColor = colors.primary
                                )
                            )
                        }

                        if (settings.enabled) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = colors.line)

                            // Lead Time Dropdown
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Reminder Time",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.text
                                    )
                                    Text(
                                        text = "Notify before class start time",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.muted
                                    )
                                }

                                Box {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = colors.surface2,
                                        border = BorderStroke(1.dp, colors.line),
                                        modifier = Modifier
                                            .clickable { showLeadTimeMenu = true }
                                            .testTag("notif_lead_time_dropdown")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${settings.leadMinutes} min before",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.primary
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                Icons.Default.ArrowDropDown,
                                                contentDescription = null,
                                                tint = colors.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = showLeadTimeMenu,
                                        onDismissRequest = { showLeadTimeMenu = false }
                                    ) {
                                        listOf(5, 10, 15, 20, 30).forEach { minutes ->
                                            DropdownMenuItem(
                                                text = { Text("$minutes minutes before") },
                                                onClick = {
                                                    viewModel.updateNotificationSettings(settings.copy(leadMinutes = minutes))
                                                    showLeadTimeMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. ADDITIONAL ALERTS
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.line)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Timetable Updates Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Timetable Update Alerts",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.text
                                )
                                Text(
                                    text = "Notify when a new schedule version is loaded",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.muted
                                )
                            }
                            Switch(
                                checked = settings.timetableUpdatesAlert,
                                onCheckedChange = { isChecked ->
                                    viewModel.updateNotificationSettings(settings.copy(timetableUpdatesAlert = isChecked))
                                },
                                modifier = Modifier.testTag("notif_updates_switch"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = colors.onPrimary,
                                    checkedTrackColor = colors.primary
                                )
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = colors.line)

                        // Morning Summary Switch & Time Picker
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Morning Summary",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.text
                                )
                                Text(
                                    text = "Daily morning overview of your classes",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.muted
                                )
                            }
                            Switch(
                                checked = settings.morningSummaryEnabled,
                                onCheckedChange = { isChecked ->
                                    viewModel.updateNotificationSettings(settings.copy(morningSummaryEnabled = isChecked))
                                },
                                modifier = Modifier.testTag("notif_morning_switch"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = colors.onPrimary,
                                    checkedTrackColor = colors.primary
                                )
                            )
                        }

                        if (settings.morningSummaryEnabled) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showTimePickerModal = true },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Summary Delivery Time",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.muted
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = colors.surface2,
                                    border = BorderStroke(1.dp, colors.line)
                                ) {
                                    Text(
                                        text = settings.morningSummaryTime,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. TEST NOTIFICATION BUTTON
            item {
                Button(
                    onClick = {
                        viewModel.sendTestNotification()
                        testNotifSentMsg = "Test notification scheduled! Check in 5 seconds."
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("send_test_notif_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Send test notification",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }

            // 5. EXPLANATORY INFO LINE
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.surface2,
                    border = BorderStroke(1.dp, colors.line),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Reminders fire automatically in the background even when app is closed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.text,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    if (showTimePickerModal) {
        val timeOptions = listOf("06:00", "06:30", "07:00", "07:30", "08:00")
        AlertDialog(
            onDismissRequest = { showTimePickerModal = false },
            title = {
                Text(
                    text = "Pick Morning Summary Time",
                    fontWeight = FontWeight.Bold,
                    color = colors.text
                )
            },
            text = {
                Column {
                    timeOptions.forEach { t ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateNotificationSettings(settings.copy(morningSummaryTime = t))
                                    showTimePickerModal = false
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = t,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (settings.morningSummaryTime == t) FontWeight.Bold else FontWeight.Normal,
                                color = colors.text
                            )
                            if (settings.morningSummaryTime == t) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = colors.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTimePickerModal = false }) {
                    Text("Close", color = colors.muted)
                }
            },
            containerColor = colors.surface
        )
    }
}

private fun checkPermissionStatus(context: Context, hasAsked: Boolean): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        return when {
            granted -> "Allowed"
            hasAsked -> "Blocked in phone settings"
            else -> "Not asked yet"
        }
    }
    return "Allowed"
}

private fun openAppSettings(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
    }
    context.startActivity(intent)
}
