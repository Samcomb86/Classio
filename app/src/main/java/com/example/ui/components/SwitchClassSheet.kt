package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.sp
import com.example.data.DepartmentsData
import com.example.model.DegreeProgram
import com.example.model.Faculty
import com.example.model.UserProfile
import com.example.ui.theme.AppTheme
import com.example.viewmodel.TimetableViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwitchClassSheet(
    viewModel: TimetableViewModel,
    onDismiss: () -> Unit
) {
    val colors = AppTheme.colors
    val recentClasses by viewModel.recentClasses.collectAsState()
    val currentProfile by viewModel.userProfile.collectAsState()

    var selectedFaculty by remember {
        mutableStateOf(
            DepartmentsData.ALL_FACULTIES.find { fac ->
                val currentDept = (currentProfile as? UserProfile.Student)?.department
                currentDept != null && (fac.name.contains(currentDept, ignoreCase = true) || currentDept.contains(fac.name, ignoreCase = true))
            } ?: DepartmentsData.FACULTY_COMPUTING
        )
    }

    var selectedProgram by remember {
        mutableStateOf<DegreeProgram?>(
            selectedFaculty.programs.firstOrNull { prog ->
                val currentProg = (currentProfile as? UserProfile.Student)?.programCode
                currentProg != null && (prog.code.equals(currentProg, ignoreCase = true) || prog.sheetCode.equals(currentProg, ignoreCase = true))
            } ?: selectedFaculty.programs.firstOrNull()
        )
    }

    var selectedSemester by remember {
        mutableStateOf<Int?>(
            (currentProfile as? UserProfile.Student)?.semester ?: 1
        )
    }

    var selectedShift by remember {
        mutableStateOf<String?>(
            (currentProfile as? UserProfile.Student)?.shift ?: "regular"
        )
    }

    var selectedSection by remember {
        mutableStateOf<String?>(
            (currentProfile as? UserProfile.Student)?.section
        )
    }

    var showComingSoonDialog by remember { mutableStateOf<Faculty?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        scrimColor = colors.text.copy(alpha = 0.32f),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        modifier = Modifier.testTag("switch_class_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Switch Class / Course",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colors.text
            )
            Spacer(modifier = Modifier.height(12.dp))

            // RECENT CLASSES CHIPS
            if (recentClasses.isNotEmpty()) {
                Text(
                    text = "Recent Classes",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.muted
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(recentClasses) { rc ->
                        val label = "${rc.programCode} ${rc.semester}${getOrdinalSuffix(rc.semester)} ${if (rc.shift == "second") "SS" else "R"}${if (!rc.section.isNullOrBlank()) " ${rc.section}" else ""}"
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = colors.surface2,
                            border = BorderStroke(1.dp, colors.line),
                            modifier = Modifier
                                .clickable {
                                    viewModel.saveStudentProfile(
                                        department = rc.department,
                                        programCode = rc.programCode,
                                        programName = rc.programName,
                                        semester = rc.semester,
                                        shift = rc.shift,
                                        section = rc.section
                                    )
                                    onDismiss()
                                }
                                .testTag("recent_class_chip_${label}")
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.accentText,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
            ) {
                // 1. Department
                item {
                    Text(
                        text = "Department",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.muted
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    var isExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(
                            onClick = { isExpanded = true },
                            border = BorderStroke(1.dp, colors.line),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedFaculty.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = colors.text
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = colors.muted)
                            }
                        }

                        DropdownMenu(
                            expanded = isExpanded,
                            onDismissRequest = { isExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            DepartmentsData.ALL_FACULTIES.forEach { fac ->
                                DropdownMenuItem(
                                    text = { Text(fac.name, style = MaterialTheme.typography.bodyMedium) },
                                    onClick = {
                                        selectedFaculty = fac
                                        selectedProgram = fac.programs.firstOrNull()
                                        selectedSemester = if (selectedProgram?.isFifthSemesterEntry == true) 5 else 1
                                        selectedShift = "regular"
                                        selectedSection = null
                                        isExpanded = false
                                        if (!fac.isLive) {
                                            showComingSoonDialog = fac
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // 2. Program
                item {
                    Text(
                        text = "Degree Program",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.muted
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    var isExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(
                            onClick = { isExpanded = true },
                            border = BorderStroke(1.dp, colors.line),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedProgram?.name ?: "Select Program",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = colors.text
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = colors.muted)
                            }
                        }

                        DropdownMenu(
                            expanded = isExpanded,
                            onDismissRequest = { isExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            selectedFaculty.programs.forEach { prog ->
                                DropdownMenuItem(
                                    text = { Text(prog.name, style = MaterialTheme.typography.bodyMedium) },
                                    onClick = {
                                        selectedProgram = prog
                                        selectedSemester = if (prog.isFifthSemesterEntry) 5 else 1
                                        selectedShift = "regular"
                                        selectedSection = null
                                        isExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // 3. Semester
                item {
                    Text(
                        text = "Semester",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.muted
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val isFifthEntry = selectedProgram?.isFifthSemesterEntry == true
                    val availableSemesters = if (selectedFaculty.isLive) {
                        viewModel.getAvailableSemesters(selectedProgram?.code ?: "", selectedProgram?.sheetCode ?: selectedProgram?.code)
                    } else if (isFifthEntry) {
                        listOf(5, 6, 7, 8)
                    } else {
                        listOf(1, 2, 3, 4, 5, 6, 7, 8)
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(availableSemesters) { sem ->
                            val isSel = selectedSemester == sem
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) colors.primary else colors.surface2,
                                border = BorderStroke(1.dp, if (isSel) colors.primary else colors.line),
                                modifier = Modifier
                                    .clickable {
                                        selectedSemester = sem
                                        selectedSection = null
                                    }
                                    .testTag("sem_chip_$sem")
                            ) {
                                Text(
                                    text = "${sem}${getOrdinalSuffix(sem)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) colors.onPrimary else colors.text,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                // 4. Shift & Section
                item {
                    Text(
                        text = "Shift / Section",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.muted
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val availableShifts = if (selectedFaculty.isLive) {
                        viewModel.getAvailableShifts(selectedProgram?.code ?: "", selectedSemester ?: 1, selectedProgram?.sheetCode ?: selectedProgram?.code)
                    } else {
                        listOf("regular", "second")
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (availableShifts.contains("regular")) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            selectedShift = "regular"
                                            selectedSection = null
                                        }
                                ) {
                                    RadioButton(
                                        selected = selectedShift == "regular",
                                        onClick = {
                                            selectedShift = "regular"
                                            selectedSection = null
                                        }
                                    )
                                    Text("First Time", color = colors.text)
                                }
                            }
                            if (availableShifts.contains("second")) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            selectedShift = "second"
                                            selectedSection = null
                                        }
                                ) {
                                    RadioButton(
                                        selected = selectedShift == "second",
                                        onClick = {
                                            selectedShift = "second"
                                            selectedSection = null
                                        }
                                    )
                                    Text("Second Time", color = colors.text)
                                }
                            }
                        }

                        if (selectedShift == "second" && selectedFaculty.isLive) {
                            val availableSections = viewModel.getAvailableSections(
                                selectedProgram?.code ?: "",
                                selectedSemester ?: 1,
                                "second",
                                selectedProgram?.sheetCode ?: selectedProgram?.code
                            )
                            if (availableSections.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    availableSections.forEach { sec ->
                                        val isSecSelected = selectedSection == sec
                                        FilterChip(
                                            selected = isSecSelected,
                                            onClick = { selectedSection = sec },
                                            label = { Text("Section $sec", fontWeight = FontWeight.Bold) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = colors.primary,
                                                selectedLabelColor = colors.onPrimary,
                                                containerColor = colors.surface2,
                                                labelColor = colors.text
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            val isLiveFaculty = selectedFaculty.isLive
            val canProceed = isLiveFaculty && selectedProgram != null && selectedSemester != null && selectedShift != null

            Button(
                onClick = {
                    if (!isLiveFaculty) {
                        showComingSoonDialog = selectedFaculty
                        return@Button
                    }
                    val prog = selectedProgram ?: return@Button
                    val sem = selectedSemester ?: return@Button
                    val shift = selectedShift ?: return@Button

                    viewModel.saveStudentProfile(
                        department = selectedFaculty.name,
                        programCode = prog.sheetCode ?: prog.code,
                        programName = prog.name,
                        semester = sem,
                        shift = shift,
                        section = if (shift == "second") selectedSection else null
                    )
                    onDismiss()
                },
                enabled = canProceed,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("apply_switch_class_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                    disabledContainerColor = colors.surface2,
                    disabledContentColor = colors.muted
                )
            ) {
                Text(
                    text = "Show Timetable",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showComingSoonDialog != null) {
        val faculty = showComingSoonDialog!!
        AlertDialog(
            onDismissRequest = { showComingSoonDialog = null },
            title = { Text(text = "${faculty.name} Schedule", fontWeight = FontWeight.Bold, color = colors.text) },
            text = {
                Text(
                    text = "The timetable for ${faculty.name} is currently being prepared for online release by Thal University Bhakkar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.text
                )
            },
            confirmButton = {
                Button(
                    onClick = { showComingSoonDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary)
                ) {
                    Text("OK")
                }
            },
            containerColor = colors.surface
        )
    }
}

private fun getOrdinalSuffix(value: Int): String {
    return when {
        value % 100 in 11..13 -> "th"
        value % 10 == 1 -> "st"
        value % 10 == 2 -> "nd"
        value % 10 == 3 -> "rd"
        else -> "th"
    }
}
