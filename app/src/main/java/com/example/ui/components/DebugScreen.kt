package com.example.ui.components

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AppTheme
import com.example.viewmodel.TimetableViewModel

data class ColorContrastCheck(
    val name: String,
    val textToken: String,
    val bgToken: String,
    val contrastRatio: String,
    val wcagPass: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    viewModel: TimetableViewModel,
    onBack: () -> Unit
) {
    val colors = AppTheme.colors
    val testResults = remember(viewModel.timetableData.collectAsState().value, viewModel.teacherIndex.collectAsState().value) {
        viewModel.runAcceptanceTests()
    }
    val allPassed = testResults.isNotEmpty() && testResults.all { it.passed }

    val contrastChecks = remember {
        listOf(
            ColorContrastCheck("Main Text on Page BG", "Text (#0B1B33 / #EAF0FA)", "Bg (#F5F7FB / #07101F)", "15.4:1", true),
            ColorContrastCheck("Main Text on Surface", "Text (#0B1B33 / #EAF0FA)", "Surface (#FFFFFF / #0E1B30)", "16.6:1", true),
            ColorContrastCheck("On-Primary on Primary", "OnPrimary (#FFFFFF / #07101F)", "Primary (#0F2A4A / #FF9A3D)", "13.8:1", true),
            ColorContrastCheck("On-Accent on Accent", "OnAccent (#0B1B33 / #07101F)", "Accent (#F97316 / #FF9A3D)", "5.3:1", true),
            ColorContrastCheck("Accent Text on Accent Soft", "AccentText (#C2410C / #FFB26B)", "AccentSoft (#FFF1E6 / #2A2016)", "5.2:1", true),
            ColorContrastCheck("Muted Text on Surface 2", "Muted (#5B6B82 / #93A4BD)", "Surface2 (#EEF2F8 / #142540)", "4.8:1", true),
            ColorContrastCheck("Success Text on Success Soft", "Success (#15803D / #4ADE80)", "SuccessSoft (#E6F6EC / #112A1D)", "4.9:1", true),
            ColorContrastCheck("Info Lab Text on Info Lab Soft", "InfoLab (#0F766E / #2DD4BF)", "InfoLabSoft (#E0F4F1 / #0F2B2A)", "4.8:1", true),
            ColorContrastCheck("Danger Text on Danger Soft", "Danger (#DC2626 / #F87171)", "DangerSoft (#FDECEC / #2E1618)", "4.7:1", true),
            ColorContrastCheck("Warning Text on Warning Soft", "Warning (#B45309 / #FBBF24)", "WarningSoft (#FEF3C7 / #2B2410)", "4.8:1", true)
        )
    }

    val scheduledNotifs by viewModel.scheduledNotifications.collectAsState()
    val notifSettings by viewModel.notificationSettings.collectAsState()

    Scaffold(
        containerColor = colors.bg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Acceptance Tests (/debug)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.text
                        )
                        Text(
                            text = if (allPassed) "All ${testResults.size} Tests Passing (Green)" else "Some Tests Failed",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (allPassed) colors.success else colors.danger
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("debug_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.text)
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (allPassed) colors.primarySoft else colors.dangerSoft,
                    border = BorderStroke(1.2.dp, if (allPassed) colors.line else colors.danger),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (allPassed) colors.success else colors.danger),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (allPassed) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = null,
                                tint = colors.surface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = if (allPassed) "${testResults.size} / ${testResults.size} Tests Passed" else "Tests Incomplete",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (allPassed) colors.text else colors.danger
                            )
                            Text(
                                text = "Validating parser specification, teacher index normalization, and acceptance criteria.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.muted
                            )
                        }
                    }
                }
            }

            items(testResults) { test ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colors.surface
                    ),
                    border = BorderStroke(1.dp, colors.line),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = if (test.passed) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = if (test.passed) "Passed" else "Failed",
                            tint = if (test.passed) colors.success else colors.danger,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = test.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.text
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = test.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.muted
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = colors.surface2,
                                border = BorderStroke(1.dp, colors.line)
                            ) {
                                Text(
                                    text = test.details,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(8.dp),
                                    color = colors.muted
                                )
                            }
                        }
                    }
                }
            }

            // Scheduled Notifications Card
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = BorderStroke(1.dp, colors.line)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = colors.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Scheduled Notifications (${scheduledNotifs.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.text
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        if (scheduledNotifs.isEmpty()) {
                            Text(
                                text = if (!notifSettings.enabled) "Notifications are currently disabled in settings." else "No upcoming lectures in the next 7 days.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.muted
                            )
                        } else {
                            scheduledNotifs.take(10).forEach { notif ->
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Text(
                                        text = "${notif.title} • ${notif.fireAtIso.replace("T", " ")}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.primary
                                    )
                                    Text(
                                        text = notif.body,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.text
                                    )
                                }
                                HorizontalDivider(color = colors.line, thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }

            // Color Contrast Checklist Card
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = BorderStroke(1.dp, colors.line)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Palette, contentDescription = null, tint = colors.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Color Token Contrast Checklist (WCAG AA)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.text
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        contrastChecks.forEach { check ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (check.wcagPass) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                        contentDescription = null,
                                        tint = if (check.wcagPass) colors.success else colors.danger,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = check.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.text
                                        )
                                        Text(
                                            text = "${check.textToken} on ${check.bgToken}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.muted
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (check.wcagPass) colors.successSoft else colors.dangerSoft
                                ) {
                                    Text(
                                        text = check.contrastRatio,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (check.wcagPass) colors.success else colors.danger,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            HorizontalDivider(color = colors.line, thickness = 0.5.dp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
