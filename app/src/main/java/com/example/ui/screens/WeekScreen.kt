package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Lecture
import com.example.model.toFilter
import com.example.ui.theme.AppTheme
import com.example.viewmodel.TimetableViewModel

@Composable
fun WeekScreen(
    viewModel: TimetableViewModel,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val context = LocalContext.current
    val timetableData by viewModel.timetableData.collectAsState()
    val currentProfile by viewModel.userProfile.collectAsState()

    val classId = when (currentProfile) {
        is com.example.model.UserProfile.Student -> (currentProfile as com.example.model.UserProfile.Student).toFilter().classId
        else -> null
    }

    val classTimetable = remember(classId, timetableData) {
        if (classId != null) {
            timetableData?.timetable?.get(classId) ?: emptyMap()
        } else {
            emptyMap()
        }
    }

    val daysList = timetableData?.days ?: listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
    val todayName = remember { TimetableViewModel.getKarachiDefaultDay() }

    // Map expanded state for each day (start with today expanded)
    val expandedStates = remember(classId) {
        mutableStateMapOf<String, Boolean>().apply {
            daysList.forEach { day ->
                this[day] = day.equals(todayName, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Weekly Schedule",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colors.text
            )

            // Share Button
            FilledIconButton(
                onClick = {
                    shareWeeklyTimetable(context, currentProfile?.toString() ?: "Timetable", daysList, classTimetable)
                },
                shape = RoundedCornerShape(12.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = colors.primarySoft,
                    contentColor = colors.accentText
                ),
                modifier = Modifier.testTag("share_week_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share Week",
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(daysList) { day ->
                val isToday = day.equals(todayName, ignoreCase = true)
                val lectures = classTimetable[day] ?: emptyList()
                val isExpanded = expandedStates[day] ?: false

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.surface,
                    border = BorderStroke(1.dp, if (isToday) colors.primary else colors.line),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedStates[day] = !isExpanded }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = day,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isToday) colors.accentText else colors.text
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(100.dp),
                                    color = colors.surface2,
                                    border = BorderStroke(1.dp, colors.line)
                                ) {
                                    Text(
                                        text = "${lectures.size} classes",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.muted,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = colors.muted
                            )
                        }

                        AnimatedVisibility(visible = isExpanded) {
                            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                                if (lectures.isEmpty()) {
                                    Text(
                                        text = "No lectures scheduled",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.muted,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                } else {
                                    lectures.forEachIndexed { idx, lecture ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = lecture.subject,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = colors.text
                                                )
                                                Text(
                                                    text = "${lecture.start} - ${lecture.end}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = colors.muted
                                                )
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = colors.primarySoft
                                            ) {
                                                Text(
                                                    text = lecture.room ?: "TBA",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.accentText,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }

                                        if (idx < lectures.size - 1) {
                                            HorizontalDivider(color = colors.line, thickness = 0.5.dp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

private fun shareWeeklyTimetable(
    context: Context,
    profileName: String,
    daysList: List<String>,
    classTimetable: Map<String, List<Lecture>>
) {
    val builder = StringBuilder()
    builder.append("Weekly Schedule for $profileName\n\n")

    for (day in daysList) {
        val lectures = classTimetable[day] ?: emptyList()
        builder.append("*$day* (${lectures.size} Classes):\n")
        if (lectures.isEmpty()) {
            builder.append("  No lectures scheduled\n")
        } else {
            lectures.forEach { lecture ->
                builder.append("  - ${lecture.start} to ${lecture.end} | ${lecture.subject} | Room ${lecture.room ?: "TBA"} | ${lecture.teacher ?: "TBA"}\n")
            }
        }
        builder.append("\n")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Weekly Timetable")
        putExtra(Intent.EXTRA_TEXT, builder.toString())
    }
    context.startActivity(Intent.createChooser(intent, "Share Timetable"))
}
