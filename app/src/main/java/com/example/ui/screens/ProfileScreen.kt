package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.config.AppConfig
import com.example.model.UserProfile
import com.example.ui.components.AdminSourceDialog
import com.example.ui.theme.AppTheme
import com.example.util.FormatUtils.getOrdinal
import com.example.viewmodel.TimetableViewModel
import com.example.viewmodel.TimetableViewMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: TimetableViewModel,
    onBack: () -> Unit
) {
    val colors = AppTheme.colors
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val lastCheckedTime by viewModel.lastCheckedTime.collectAsState()
    val isLiveSource by viewModel.isLiveSource.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val showAdminDialog by viewModel.showAdminDialog.collectAsState()

    val currentUser by viewModel.currentUser.collectAsState()
    val customUserName by viewModel.customUserName.collectAsState()
    val customProfileImage by viewModel.customProfileImage.collectAsState()

    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var isEditingName by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf(customUserName) }

    // Launcher for selecting profile photo
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.updateCustomProfileImage(uri.toString())
        }
    }

    BackHandler {
        onBack()
    }

    Scaffold(
        containerColor = colors.bg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "My Profile & Settings",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.text
                        )
                        Text(
                            text = "Class Flow • Thal University Bhakkar",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.muted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("profile_back_button")
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { Spacer(modifier = Modifier.height(2.dp)) }

            // 1. GOOGLE ACCOUNT & AVATAR EDITOR CARD
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.line),
                    modifier = Modifier.fillMaxWidth().testTag("profile_google_identity_card")
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Editable Avatar Circle
                        Box(
                            contentAlignment = Alignment.BottomEnd,
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                        ) {
                            if (customProfileImage.isNotBlank()) {
                                AsyncImage(
                                    model = customProfileImage,
                                    contentDescription = "Profile Photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Surface(
                                    color = colors.primarySoft,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = colors.primary,
                                            modifier = Modifier.size(44.dp)
                                        )
                                    }
                                }
                            }

                            // Camera overlay badge
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(colors.primary)
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Change Image",
                                    tint = colors.onPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Editable Name Field
                        if (isEditingName) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                OutlinedTextField(
                                    value = tempName,
                                    onValueChange = { tempName = it },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = colors.primary,
                                        unfocusedBorderColor = colors.line,
                                        focusedTextColor = colors.text,
                                        unfocusedTextColor = colors.text
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp)
                                        .testTag("profile_name_input_field")
                                )

                                IconButton(
                                    onClick = {
                                        if (tempName.isNotBlank()) {
                                            viewModel.updateCustomUserName(tempName)
                                        }
                                        isEditingName = false
                                    },
                                    modifier = Modifier
                                        .background(colors.primarySoft, CircleShape)
                                        .size(40.dp)
                                        .testTag("profile_name_save_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Save Name",
                                        tint = colors.accentText
                                    )
                                }
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val displayName = if (customUserName.isNotBlank()) customUserName else (currentUser?.displayName ?: "Set Name")
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.text
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Name",
                                    tint = colors.primary,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable {
                                            tempName = customUserName
                                            isEditingName = true
                                        }
                                        .testTag("profile_edit_name_pencil")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Display active email
                        val userEmail = currentUser?.email ?: "Offline Account (Google Auth Not Active)"
                        Text(
                            text = userEmail,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.muted
                        )

                        if (currentUser != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            // Disconnect / Log out Button
                            Button(
                                onClick = {
                                    viewModel.signOutUser()
                                    onBack()
                                    viewModel.clearProfileAndShowWizard()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.dangerSoft,
                                    contentColor = colors.danger
                                ),
                                border = BorderStroke(1.dp, colors.danger.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth().testTag("profile_google_logout_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Logout,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Log out of Google Account", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 2. USER ENROLLED TIMETABLE IDENTITY CARD
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_user_card"),
                    shape = RoundedCornerShape(20.dp),
                    color = colors.surface2,
                    border = BorderStroke(1.2.dp, colors.line)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        when (val profile = userProfile) {
                            is UserProfile.Student -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(colors.primarySoft),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.School,
                                            contentDescription = "Student",
                                            modifier = Modifier.size(30.dp),
                                            tint = colors.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = colors.surface,
                                            border = BorderStroke(1.dp, colors.line)
                                        ) {
                                            Text(
                                                text = "ENROLLED STUDENT",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.primary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = profile.programName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.text
                                        )
                                        Text(
                                            text = profile.department,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.muted
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = colors.line)
                                Spacer(modifier = Modifier.height(16.dp))

                                // Details chips grid
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    ProfileInfoChip(
                                        label = "Semester",
                                        value = "${profile.semester}${getOrdinal(profile.semester)} Sem",
                                        modifier = Modifier.weight(1f)
                                    )
                                    val shiftLabel = if (profile.shift == "regular") "First Time (R)" else "Second Time (SS)"
                                    ProfileInfoChip(
                                        label = "Shift",
                                        value = shiftLabel,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (profile.section != null) {
                                        ProfileInfoChip(
                                            label = "Section",
                                            value = profile.section,
                                            modifier = Modifier.weight(0.8f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                // SWITCH / CHANGE CLASS BUTTON
                                Button(
                                    onClick = {
                                        onBack()
                                        viewModel.showClassSelector(true)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("profile_switch_class_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.primary,
                                        contentColor = colors.onPrimary
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = colors.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Switch / Change Class",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = colors.onPrimary
                                    )
                                }
                            }
                            is UserProfile.Teacher -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(colors.primarySoft),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "Teacher",
                                            modifier = Modifier.size(30.dp),
                                            tint = colors.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = colors.surface,
                                            border = BorderStroke(1.dp, colors.line)
                                        ) {
                                            Text(
                                                text = "FACULTY MEMBER",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.primary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = profile.teacherName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.text
                                        )
                                        Text(
                                            text = profile.department,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.muted
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                Button(
                                    onClick = {
                                        onBack()
                                        viewModel.showWizardScreen(true)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("profile_switch_teacher_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.primary,
                                        contentColor = colors.onPrimary
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = colors.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Select Another Teacher",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = colors.onPrimary
                                    )
                                }
                            }
                            null -> {
                                Text(
                                    text = "No profile loaded",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.muted
                                )
                            }
                        }
                    }
                }
            }

            // 3. SETTINGS SECTION
            item {
                Text(
                    text = "SETTINGS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.line)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // View Mode setting
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Schedule View Mode",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.text
                                )
                                Text(
                                    text = if (viewMode == TimetableViewMode.DAY_VIEW) "Day-by-Day View" else "Full Week View",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.muted
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = viewMode == TimetableViewMode.DAY_VIEW,
                                    onClick = { viewModel.setViewMode(TimetableViewMode.DAY_VIEW) },
                                    label = { Text("Day") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = colors.primary,
                                        selectedLabelColor = colors.onPrimary,
                                        containerColor = colors.surface2,
                                        labelColor = colors.text
                                    )
                                )
                                FilterChip(
                                    selected = viewMode == TimetableViewMode.FULL_WEEK_VIEW,
                                    onClick = { viewModel.setViewMode(TimetableViewMode.FULL_WEEK_VIEW) },
                                    label = { Text("Week") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = colors.primary,
                                        selectedLabelColor = colors.onPrimary,
                                        containerColor = colors.surface2,
                                        labelColor = colors.text
                                    )
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = colors.line)

                        // Timetable Data Sync & Refresh
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Timetable Sync Status",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.text
                                )
                                Text(
                                    text = if (isLiveSource) "Online (Live sync enabled)" else "Offline Bundled Copy (Fall 2026 V1.6)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.muted
                                )
                            }
                            IconButton(
                                onClick = { viewModel.refreshTimetable() },
                                enabled = !isRefreshing,
                                modifier = Modifier.testTag("profile_refresh_button")
                            ) {
                                if (isRefreshing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = colors.primary
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = "Sync Now",
                                        tint = colors.primary
                                    )
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = colors.line)

                        // Lecture Reminders Settings Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onBack()
                                    viewModel.showNotificationSettings(true)
                                }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Lecture Reminders",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.text
                                )
                                Text(
                                    text = "Lead time, alerts, and test notifications",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.muted
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = colors.primary
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = colors.line)

                        // Change Role / Reset All Data
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showResetConfirmDialog = true }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Change Role or Reset",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.danger
                                )
                                Text(
                                    text = "Switch between Student and Teacher or reset profile",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.muted
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = colors.muted
                            )
                        }
                    }
                }
            }

            // 4. ABOUT US SECTION
            item {
                Text(
                    text = "ABOUT CLASS FLOW",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.line)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = colors.primary,
                                modifier = Modifier.size(44.dp)
                              ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = "App Icon",
                                        tint = colors.onPrimary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Class Flow",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.text
                                )
                                Text(
                                    text = "Thal University Bhakkar Timetable Assistant",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.muted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Class Flow is designed for students and faculty of Thal University Bhakkar (tu.edu.pk). It parses university-wide schedules so you find your personal daily classes in 4 taps or fewer.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.text,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = colors.line)
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Timetable Release",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.muted
                            )
                            Text(
                                text = "Fall 2026 (V1.6)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.text
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // Reset Confirmation Dialog
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = {
                Text(
                    text = "Reset Profile?",
                    fontWeight = FontWeight.Bold,
                    color = colors.text
                )
            },
            text = {
                Text(
                    text = "This will return to the welcome wizard so you can pick a different role, department, or degree program.",
                    color = colors.muted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirmDialog = false
                        onBack()
                        viewModel.clearProfileAndShowWizard()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.danger,
                        contentColor = colors.surface
                    )
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancel", color = colors.muted)
                }
            },
            containerColor = colors.surface
        )
    }

    if (showAdminDialog) {
        AdminSourceDialog(
            initialUrl = viewModel.getCustomSourceUrl(),
            onDismiss = { viewModel.showAdminSettings(false) },
            onSave = { newUrl -> viewModel.saveCustomSourceUrl(newUrl) }
        )
    }
}

@Composable
private fun ProfileInfoChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = colors.surface2,
        border = BorderStroke(1.dp, colors.line)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = colors.muted
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = colors.text
            )
        }
    }
}
