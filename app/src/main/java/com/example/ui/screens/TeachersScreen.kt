package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.sp
import com.example.model.TeacherInfo
import com.example.ui.theme.AppTheme
import com.example.viewmodel.TimetableViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeachersScreen(
    viewModel: TimetableViewModel,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val teacherIndex by viewModel.teacherIndex.collectAsState()
    val timetableData by viewModel.timetableData.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTeacher by remember { mutableStateOf<TeacherInfo?>(null) }

    val filteredTeachers = remember(teacherIndex, searchQuery) {
        val list = teacherIndex?.teachers ?: emptyList()
        if (searchQuery.isBlank()) {
            list.sortedBy { it.name }
        } else {
            list.filter { it.name.contains(searchQuery, ignoreCase = true) }.sortedBy { it.name }
        }
    }

    if (selectedTeacher != null) {
        val teacher = selectedTeacher!!
        BackHandler { selectedTeacher = null }

        Scaffold(
            containerColor = colors.bg,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = teacher.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = colors.text
                            )
                            Text(
                                text = teacher.department,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.muted
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { selectedTeacher = null }, modifier = Modifier.testTag("teacher_detail_back")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.text)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.bg)
                )
            }
        ) { paddingValues ->
            val teacherSchedule = remember(teacher) {
                teacherIndex?.byTeacher?.get(teacher.key) ?: emptyMap()
            }
            val daysList = timetableData?.days ?: listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                items(daysList) { day ->
                    val dayLectures = teacherSchedule[day] ?: emptyList()

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.surface,
                        border = BorderStroke(1.dp, colors.line),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = day,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.accentText
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            if (dayLectures.isEmpty()) {
                                Text(
                                    text = "No lectures scheduled on this day.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.muted
                                )
                            } else {
                                dayLectures.forEachIndexed { index, lec ->
                                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = lec.subject,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colors.text,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = colors.primarySoft
                                            ) {
                                                Text(
                                                    text = lec.room ?: "TBA",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.accentText,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${lec.start} - ${lec.end}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colors.muted
                                            )
                                            Text(
                                                text = lec.classLabels.joinToString(", "),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colors.primary
                                            )
                                        }
                                    }

                                    if (index < dayLectures.size - 1) {
                                        HorizontalDivider(color = colors.line, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 6.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Based on the current timetable version ${timetableData?.meta?.version ?: "V1.6"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.muted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = "Teachers Directory",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colors.text
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Search query field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search teacher name", color = colors.muted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = colors.muted) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.line,
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    focusedTextColor = colors.text,
                    unfocusedTextColor = colors.text
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("teachers_search_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (filteredTeachers.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No teachers found matching \"$searchQuery\"", color = colors.muted)
                        }
                    }
                } else {
                    items(filteredTeachers) { teacher ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = colors.surface,
                            border = BorderStroke(1.dp, colors.line),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTeacher = teacher }
                                .testTag("teacher_row_${teacher.key}")
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = colors.primarySoft,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = colors.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = teacher.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.text
                                        )
                                        Text(
                                            text = teacher.department,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.muted
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(100.dp),
                                    color = colors.surface2,
                                    border = BorderStroke(1.dp, colors.line)
                                ) {
                                    Text(
                                        text = "${teacher.lectureCount} classes",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }
}
