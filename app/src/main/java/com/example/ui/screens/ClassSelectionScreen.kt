package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DepartmentsData
import com.example.model.DegreeProgram
import com.example.model.Faculty
import com.example.ui.theme.AppTheme
import com.example.util.FormatUtils.getOrdinal
import com.example.viewmodel.TimetableViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassSelectionScreen(
    viewModel: TimetableViewModel,
    onBack: () -> Unit
) {
    val colors = AppTheme.colors
    val timetableData by viewModel.timetableData.collectAsState()
    val currentProfile by viewModel.userProfile.collectAsState()

    // Initialize with current selection if available
    var selectedFaculty by remember {
        mutableStateOf(
            DepartmentsData.ALL_FACULTIES.find { fac ->
                val currentDept = (currentProfile as? com.example.model.UserProfile.Student)?.department
                currentDept != null && (fac.name.contains(currentDept, ignoreCase = true) || currentDept.contains(fac.name, ignoreCase = true))
            } ?: DepartmentsData.FACULTY_COMPUTING
        )
    }

    var selectedProgram by remember {
        mutableStateOf<DegreeProgram?>(
            selectedFaculty.programs.firstOrNull { prog ->
                val currentProg = (currentProfile as? com.example.model.UserProfile.Student)?.programCode
                currentProg != null && (prog.code.equals(currentProg, ignoreCase = true) || prog.sheetCode.equals(currentProg, ignoreCase = true))
            } ?: selectedFaculty.programs.firstOrNull()
        )
    }

    var selectedSemester by remember {
        mutableStateOf<Int?>(
            (currentProfile as? com.example.model.UserProfile.Student)?.semester ?: 1
        )
    }

    var selectedShift by remember {
        mutableStateOf<String?>(
            (currentProfile as? com.example.model.UserProfile.Student)?.shift ?: "regular"
        )
    }

    var selectedSection by remember {
        mutableStateOf<String?>(
            (currentProfile as? com.example.model.UserProfile.Student)?.section
        )
    }

    // Coming soon notice dialog
    var showComingSoonDialog by remember { mutableStateOf<Faculty?>(null) }

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
                            text = "Select Class & Department",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.text
                        )
                        Text(
                            text = "Choose your faculty, course, semester and shift",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.muted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("class_selector_back_button")
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
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = colors.bg,
                border = BorderStroke(1.dp, colors.line),
                shadowElevation = 8.dp
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
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
                            onBack()
                        },
                        enabled = canProceed,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("apply_class_selection_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = colors.onPrimary,
                            disabledContainerColor = colors.surface2,
                            disabledContentColor = colors.muted
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (canProceed) colors.onPrimary else colors.muted
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isLiveFaculty) "Apply & Open Timetable" else "Faculty Coming Soon",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (canProceed) colors.onPrimary else colors.muted
                        )
                    }
                }
            }
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

            // FACULTY / DEPARTMENT SELECTION
            item {
                SectionHeader(
                    stepNumber = "1",
                    title = "Faculty / Department",
                    subtitle = "Select your academic faculty"
                )
                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DepartmentsData.ALL_FACULTIES.forEach { faculty ->
                        val isSelected = faculty.id == selectedFaculty.id
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    selectedFaculty = faculty
                                    selectedProgram = faculty.programs.firstOrNull()
                                    selectedSemester = if (selectedProgram?.isFifthSemesterEntry == true) 5 else 1
                                    selectedShift = "regular"
                                    selectedSection = null
                                    if (!faculty.isLive) {
                                        showComingSoonDialog = faculty
                                    }
                                }
                                .testTag("faculty_item_${faculty.id}"),
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) colors.primarySoft else colors.surface,
                            border = BorderStroke(if (isSelected) 1.8.dp else 1.dp, if (isSelected) colors.primary else colors.line)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        selectedFaculty = faculty
                                        selectedProgram = faculty.programs.firstOrNull()
                                        selectedSemester = if (selectedProgram?.isFifthSemesterEntry == true) 5 else 1
                                        selectedShift = "regular"
                                        selectedSection = null
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = colors.primary,
                                        unselectedColor = colors.muted
                                    )
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = faculty.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            color = colors.text,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        if (faculty.isLive) {
                                            Surface(
                                                shape = RoundedCornerShape(100.dp),
                                                color = colors.successSoft,
                                                border = BorderStroke(1.dp, colors.success)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .clip(CircleShape)
                                                            .background(colors.success)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "LIVE",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                        fontWeight = FontWeight.Bold,
                                                        color = colors.success
                                                    )
                                                }
                                            }
                                        } else {
                                            Surface(
                                                shape = RoundedCornerShape(100.dp),
                                                color = colors.accentSoft,
                                                border = BorderStroke(1.dp, colors.accent)
                                            ) {
                                                Text(
                                                    text = "Coming Soon",
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                    fontWeight = FontWeight.Medium,
                                                    color = colors.accentText
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${faculty.programs.size} Degree Programs",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.muted
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // PROGRAM / DEGREE SELECTION
            item {
                SectionHeader(
                    stepNumber = "2",
                    title = "Degree Program",
                    subtitle = "Select your degree under ${selectedFaculty.name}"
                )
                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    selectedFaculty.programs.forEach { program ->
                        val isSelected = selectedProgram?.code == program.code
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedProgram = program
                                    selectedSemester = if (program.isFifthSemesterEntry) 5 else 1
                                    selectedShift = "regular"
                                    selectedSection = null
                                }
                                .testTag("program_item_${program.code}"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) colors.primarySoft else colors.surface,
                            border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) colors.primary else colors.line)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        selectedProgram = program
                                        selectedSemester = if (program.isFifthSemesterEntry) 5 else 1
                                        selectedShift = "regular"
                                        selectedSection = null
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = colors.primary,
                                        unselectedColor = colors.muted
                                    )
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = program.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = colors.text
                                    )
                                    if (program.isFifthSemesterEntry) {
                                        Text(
                                            text = "Lateral Entry / 5th Semester Program",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SEMESTER OPTION
            item {
                SectionHeader(
                    stepNumber = "3",
                    title = "Semester",
                    subtitle = "Select your current enrolled semester"
                )
                Spacer(modifier = Modifier.height(10.dp))

                val programCode = selectedProgram?.code ?: ""
                val sheetCode = selectedProgram?.sheetCode ?: selectedProgram?.code
                val isFifthEntry = selectedProgram?.isFifthSemesterEntry == true

                val availableSemesters = remember(programCode, timetableData) {
                    if (selectedFaculty.isLive) {
                        viewModel.getAvailableSemesters(programCode, sheetCode)
                    } else if (isFifthEntry) {
                        listOf(5, 6, 7, 8)
                    } else {
                        listOf(1, 2, 3, 4, 5, 6, 7, 8)
                    }
                }

                // Grid of Semesters
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val rows = availableSemesters.chunked(4)
                    rows.forEach { rowSemesters ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowSemesters.forEach { sem ->
                                val isSelected = selectedSemester == sem
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            selectedSemester = sem
                                            selectedSection = null
                                        }
                                        .testTag("semester_chip_$sem"),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) colors.primary else colors.surface2,
                                    border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) colors.primary else colors.line)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(
                                            text = "${sem}${getOrdinal(sem)}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isSelected) colors.onPrimary else colors.text
                                        )
                                    }
                                }
                            }
                            for (i in 0 until (4 - rowSemesters.size)) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // SHIFT OPTION (Morning & Afternoon)
            item {
                SectionHeader(
                    stepNumber = "4",
                    title = "Shift (Morning / Afternoon)",
                    subtitle = "First Time is Regular (R) • Second Time is Self Study (SS)"
                )
                Spacer(modifier = Modifier.height(10.dp))

                val programCode = selectedProgram?.code ?: ""
                val sheetCode = selectedProgram?.sheetCode ?: selectedProgram?.code
                val sem = selectedSemester ?: 1

                val availableShifts = remember(programCode, sem, timetableData) {
                    if (selectedFaculty.isLive) {
                        viewModel.getAvailableShifts(programCode, sem, sheetCode)
                    } else {
                        listOf("regular", "second")
                    }
                }
                val isRegularAvailable = availableShifts.contains("regular")
                val isSecondAvailable = availableShifts.contains("second")

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ShiftSelectionCard(
                        title = "First Time (Regular - R)",
                        subtitle = "Standard Morning/Afternoon Classes (R)",
                        isSelected = selectedShift == "regular",
                        isAvailable = isRegularAvailable,
                        tag = "shift_morning_r",
                        onClick = {
                            selectedShift = "regular"
                            selectedSection = null
                        }
                    )

                    ShiftSelectionCard(
                        title = "Second Time (Self Study - SS)",
                        subtitle = "Afternoon Classes (SS1, SS2, etc.)",
                        isSelected = selectedShift == "second",
                        isAvailable = isSecondAvailable,
                        tag = "shift_afternoon_ss",
                        onClick = {
                            selectedShift = "second"
                            selectedSection = null
                        }
                    )
                }

                if (selectedShift == "second" && selectedFaculty.isLive) {
                    val availableSections = remember(programCode, sem, timetableData) {
                        viewModel.getAvailableSections(programCode, sem, "second", sheetCode)
                    }
                    if (availableSections.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Select Section",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.text
                        )
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

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    if (showComingSoonDialog != null) {
        val faculty = showComingSoonDialog!!
        AlertDialog(
            onDismissRequest = { showComingSoonDialog = null },
            title = {
                Text(
                    text = "${faculty.name} Schedule",
                    fontWeight = FontWeight.Bold,
                    color = colors.text
                )
            },
            text = {
                Column {
                    Text(
                        text = "The timetable for ${faculty.name} is currently being prepared for online release by Thal University Bhakkar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.text
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Included programs (${faculty.programs.size}):",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = faculty.programs.joinToString(", ") { it.name },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showComingSoonDialog = null },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary
                    )
                ) {
                    Text("OK")
                }
            },
            containerColor = colors.surface
        )
    }
}

@Composable
private fun SectionHeader(
    stepNumber: String,
    title: String,
    subtitle: String
) {
    val colors = AppTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(colors.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = colors.onPrimary
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.text
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted
            )
        }
    }
}

@Composable
private fun ShiftSelectionCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    isAvailable: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = isAvailable) { onClick() }
            .testTag(tag),
        shape = RoundedCornerShape(14.dp),
        color = when {
            !isAvailable -> colors.surface2.copy(alpha = 0.5f)
            isSelected -> colors.primarySoft
            else -> colors.surface
        },
        border = BorderStroke(
            if (isSelected) 1.8.dp else 1.dp,
            if (isSelected) colors.primary else colors.line
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = if (isAvailable) onClick else null,
                enabled = isAvailable,
                colors = RadioButtonDefaults.colors(
                    selectedColor = colors.primary,
                    unselectedColor = colors.muted
                )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (!isAvailable) colors.muted.copy(alpha = 0.5f) else colors.text
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isAvailable) subtitle else "No lectures scheduled for this shift",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted
                )
            }
        }
    }
}
