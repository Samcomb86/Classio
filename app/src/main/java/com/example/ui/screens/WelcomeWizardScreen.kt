package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DepartmentsData
import com.example.model.DegreeProgram
import com.example.model.Department
import com.example.model.TeacherInfo
import com.example.ui.theme.AppTheme
import com.example.util.FormatUtils.getOrdinal
import com.example.viewmodel.TimetableViewModel

enum class WizardStep {
    ROLE_SELECTION,
    STUDENT_DEPARTMENT,
    STUDENT_PROGRAM,
    STUDENT_SEMESTER_SHIFT,
    STUDENT_SECTION,
    TEACHER_DEPARTMENT,
    TEACHER_SELECT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeWizardScreen(
    viewModel: TimetableViewModel
) {
    val colors = AppTheme.colors
    val timetableData by viewModel.timetableData.collectAsState()
    val teacherIndex by viewModel.teacherIndex.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val firebaseStatus by viewModel.firebaseStatus.collectAsState()

    var currentStep by remember { mutableStateOf(WizardStep.ROLE_SELECTION) }
    var selectedRole by remember { mutableStateOf<String?>(null) } // "student" or "teacher"

    // Student selections
    var studentDept by remember { mutableStateOf(DepartmentsData.LIVE_DEPARTMENT) }
    var studentProgram by remember { mutableStateOf<DegreeProgram?>(studentDept.programs.firstOrNull()) }
    var studentSemester by remember { mutableStateOf<Int?>(1) }
    var studentShift by remember { mutableStateOf<String?>("regular") } // "regular" or "second"
    var studentSection by remember { mutableStateOf<String?>(null) } // null, "SS1", "SS2"

    // Teacher selections
    var teacherDept by remember { mutableStateOf(DepartmentsData.LIVE_DEPARTMENT) }
    var selectedTeacher by remember { mutableStateOf<TeacherInfo?>(null) }
    var teacherSearchQuery by remember { mutableStateOf("") }

    // Dialog for coming soon department
    var comingSoonDeptClicked by remember { mutableStateOf<Department?>(null) }

    val canProceed = when (currentStep) {
        WizardStep.ROLE_SELECTION -> selectedRole != null
        WizardStep.STUDENT_DEPARTMENT -> true
        WizardStep.STUDENT_PROGRAM -> studentProgram != null
        WizardStep.STUDENT_SEMESTER_SHIFT -> studentSemester != null && studentShift != null
        WizardStep.STUDENT_SECTION -> true
        WizardStep.TEACHER_DEPARTMENT -> true
        WizardStep.TEACHER_SELECT -> selectedTeacher != null
    }

    val isLastStep = currentStep == WizardStep.STUDENT_SEMESTER_SHIFT || currentStep == WizardStep.STUDENT_SECTION || currentStep == WizardStep.TEACHER_SELECT

    val teachersList = teacherIndex?.teachers ?: emptyList()
    val filteredTeachers = remember(teachersList, teacherSearchQuery) {
        if (teacherSearchQuery.isBlank()) {
            teachersList
        } else {
            teachersList.filter { it.name.contains(teacherSearchQuery, ignoreCase = true) }
        }
    }

    Scaffold(
        containerColor = colors.bg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Class Flow Setup",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.text
                        )
                        Text(
                            text = "Thal University Bhakkar",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.muted
                        )
                    }
                },
                navigationIcon = {
                    if (currentStep != WizardStep.ROLE_SELECTION) {
                        IconButton(
                            onClick = {
                                currentStep = when (currentStep) {
                                    WizardStep.STUDENT_DEPARTMENT -> WizardStep.ROLE_SELECTION
                                    WizardStep.STUDENT_PROGRAM -> WizardStep.STUDENT_DEPARTMENT
                                    WizardStep.STUDENT_SEMESTER_SHIFT -> WizardStep.STUDENT_PROGRAM
                                    WizardStep.STUDENT_SECTION -> WizardStep.STUDENT_SEMESTER_SHIFT
                                    WizardStep.TEACHER_DEPARTMENT -> WizardStep.ROLE_SELECTION
                                    WizardStep.TEACHER_SELECT -> WizardStep.TEACHER_DEPARTMENT
                                    else -> WizardStep.ROLE_SELECTION
                                }
                            },
                            modifier = Modifier.testTag("wizard_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = colors.text
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.bg
                )
            )
        },
        bottomBar = {
            if (currentStep != WizardStep.ROLE_SELECTION) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.line)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                when (currentStep) {
                                    WizardStep.STUDENT_DEPARTMENT -> currentStep = WizardStep.STUDENT_PROGRAM
                                    WizardStep.STUDENT_PROGRAM -> {
                                        studentSemester = if (studentProgram?.isFifthSemesterEntry == true) 5 else 1
                                        studentShift = "regular"
                                        currentStep = WizardStep.STUDENT_SEMESTER_SHIFT
                                    }
                                    WizardStep.STUDENT_SEMESTER_SHIFT -> {
                                        if (studentShift == "second") {
                                            currentStep = WizardStep.STUDENT_SECTION
                                        } else {
                                            val prog = studentProgram ?: return@Button
                                            val sem = studentSemester ?: 1
                                            viewModel.saveStudentProfile(
                                                department = studentDept.name,
                                                programCode = prog.code,
                                                programName = prog.name,
                                                semester = sem,
                                                shift = "regular",
                                                section = null
                                            )
                                        }
                                    }
                                    WizardStep.STUDENT_SECTION -> {
                                        val prog = studentProgram ?: return@Button
                                        val sem = studentSemester ?: 1
                                        viewModel.saveStudentProfile(
                                            department = studentDept.name,
                                            programCode = prog.code,
                                            programName = prog.name,
                                            semester = sem,
                                            shift = "second",
                                            section = studentSection
                                        )
                                    }
                                    WizardStep.TEACHER_DEPARTMENT -> currentStep = WizardStep.TEACHER_SELECT
                                    WizardStep.TEACHER_SELECT -> {
                                        val teacher = selectedTeacher ?: return@Button
                                        viewModel.saveTeacherProfile(
                                            department = teacherDept.name,
                                            teacherName = teacher.name,
                                            teacherKey = teacher.key
                                        )
                                    }
                                    else -> {}
                                }
                            },
                            enabled = canProceed,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("wizard_next_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primary,
                                contentColor = colors.onPrimary,
                                disabledContainerColor = colors.surface2,
                                disabledContentColor = colors.muted
                            )
                        ) {
                            Text(
                                text = if (isLastStep) "Open Timetable" else "Next",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (canProceed) colors.onPrimary else colors.muted
                            )
                        }
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (currentStep) {
                // STEP 0: "Who are you?" with Gradient Role Cards
                WizardStep.ROLE_SELECTION -> {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Who are you?",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.text
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Underline gradient from primary to accent
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(colors.primary, colors.accent)
                                    )
                                )
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Select your role to quickly find your own timetable in 4 taps or fewer.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.muted
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // STEP 5: Student Option Card (Gradients stay consistent in light/dark)
                    item {
                        val isSelected = selectedRole == "student"
                        val studentGradient = Brush.verticalGradient(
                            colors = listOf(Color(0xFF1E40AF), Color(0xFF0F2A4A))
                        )

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    selectedRole = "student"
                                    currentStep = WizardStep.STUDENT_DEPARTMENT
                                }
                                .testTag("role_student_card"),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(if (isSelected) 2.5.dp else 1.dp, if (isSelected) colors.accent else colors.line)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(studentGradient)
                                    .padding(22.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.School,
                                            contentDescription = "Student",
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(18.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Student",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Find schedule for your class, subjects, and rooms",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White.copy(alpha = 0.85f)
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = colors.accent,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // STEP 5: Teacher Option Card
                    item {
                        val isSelected = selectedRole == "teacher"
                        val teacherGradient = Brush.verticalGradient(
                            colors = listOf(Color(0xFFFB923C), Color(0xFFC2410C))
                        )

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    selectedRole = "teacher"
                                    currentStep = WizardStep.TEACHER_DEPARTMENT
                                }
                                .testTag("role_teacher_card"),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(if (isSelected) 2.5.dp else 1.dp, if (isSelected) colors.accent else colors.line)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(teacherGradient)
                                    .padding(22.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CoPresent,
                                            contentDescription = "Teacher",
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(18.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Teacher / Faculty",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "View all your assigned lectures across degree programs",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White.copy(alpha = 0.85f)
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = colors.accent,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // FIREBASE AUTH AND SYNC CARD
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = colors.surface,
                            border = BorderStroke(1.dp, colors.line),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("firebase_auth_card")
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(colors.primarySoft),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudQueue,
                                            contentDescription = null,
                                            tint = colors.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Account & Cloud Sync",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.text
                                        )
                                        Text(
                                            text = "Sync timetable schedule and settings across devices",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.muted
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                val user = currentUser
                                if (user != null) {
                                    // User is signed in!
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = colors.primarySoft,
                                        border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.3f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(colors.primary),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (user.isGoogleAccount) Icons.Default.AccountCircle else Icons.Default.CloudDone,
                                                    contentDescription = null,
                                                    tint = colors.onPrimary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = if (user.isGoogleAccount) "Signed in with Google" else "Signed in with Firebase",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.accentText
                                                )
                                                Text(
                                                    text = user.displayName ?: user.email ?: "Active Account",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = colors.text
                                                )
                                                if (user.email != null) {
                                                    Text(
                                                        text = user.email,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = colors.muted
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    TextButton(
                                        onClick = { viewModel.signOutUser() },
                                        modifier = Modifier
                                            .align(Alignment.End)
                                            .testTag("firebase_sign_out_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Logout,
                                            contentDescription = null,
                                            tint = colors.danger,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "Log Out",
                                            color = colors.danger,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    // User is not signed in
                                    var email by remember { mutableStateOf("") }
                                    var password by remember { mutableStateOf("") }
                                    var confirmPassword by remember { mutableStateOf("") }
                                    var showPassword by remember { mutableStateOf(false) }
                                    var isSignUpMode by remember { mutableStateOf(false) }
                                    var statusMsg by remember { mutableStateOf("") }
                                    var isError by remember { mutableStateOf(false) }
                                    var loading by remember { mutableStateOf(false) }
                                    var showForgotDialog by remember { mutableStateOf(false) }
                                    var forgotEmail by remember { mutableStateOf("") }
                                    var forgotMsg by remember { mutableStateOf("") }
                                    var forgotError by remember { mutableStateOf(false) }

                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        // Mode Switch (Sign In vs Sign Up Tabs)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(colors.surface2)
                                                .padding(4.dp)
                                        ) {
                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        isSignUpMode = false
                                                        statusMsg = ""
                                                    },
                                                color = if (!isSignUpMode) colors.primary else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier.padding(vertical = 10.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "Sign In",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (!isSignUpMode) colors.onPrimary else colors.muted
                                                    )
                                                }
                                            }

                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        isSignUpMode = true
                                                        statusMsg = ""
                                                    },
                                                color = if (isSignUpMode) colors.primary else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier.padding(vertical = 10.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "Create Account",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSignUpMode) colors.onPrimary else colors.muted
                                                    )
                                                }
                                            }
                                        }

                                        // Email Input
                                        OutlinedTextField(
                                            value = email,
                                            onValueChange = {
                                                email = it
                                                statusMsg = ""
                                            },
                                            label = { Text("Email Address", color = colors.muted) },
                                            placeholder = { Text("student@tu.edu.pk", color = colors.muted.copy(alpha = 0.5f)) },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Email,
                                                    contentDescription = "Email Icon",
                                                    tint = colors.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = colors.primary,
                                                unfocusedBorderColor = colors.line,
                                                focusedTextColor = colors.text,
                                                unfocusedTextColor = colors.text
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("firebase_email_input")
                                        )

                                        // Password Input
                                        OutlinedTextField(
                                            value = password,
                                            onValueChange = {
                                                password = it
                                                statusMsg = ""
                                            },
                                            label = { Text("Password", color = colors.muted) },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Lock,
                                                    contentDescription = "Password Icon",
                                                    tint = colors.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            },
                                            trailingIcon = {
                                                IconButton(onClick = { showPassword = !showPassword }) {
                                                    Icon(
                                                        imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                        contentDescription = if (showPassword) "Hide Password" else "Show Password",
                                                        tint = colors.muted,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            },
                                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = colors.primary,
                                                unfocusedBorderColor = colors.line,
                                                focusedTextColor = colors.text,
                                                unfocusedTextColor = colors.text
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("firebase_password_input")
                                        )

                                        // Confirm Password Input (Sign Up mode only)
                                        if (isSignUpMode) {
                                            OutlinedTextField(
                                                value = confirmPassword,
                                                onValueChange = {
                                                    confirmPassword = it
                                                    statusMsg = ""
                                                },
                                                label = { Text("Confirm Password", color = colors.muted) },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Confirm Icon",
                                                        tint = colors.primary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                },
                                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                                singleLine = true,
                                                shape = RoundedCornerShape(12.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = colors.primary,
                                                    unfocusedBorderColor = colors.line,
                                                    focusedTextColor = colors.text,
                                                    unfocusedTextColor = colors.text
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("firebase_confirm_password_input")
                                            )
                                        }

                                        // Forgot Password link (Sign In mode only)
                                        if (!isSignUpMode) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                Text(
                                                    text = "Forgot Password?",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = colors.primary,
                                                    modifier = Modifier
                                                        .clickable {
                                                            forgotEmail = email
                                                            forgotMsg = ""
                                                            showForgotDialog = true
                                                        }
                                                        .padding(vertical = 2.dp)
                                                        .testTag("firebase_forgot_password_button")
                                                )
                                            }
                                        }

                                        // Status message banner
                                        if (statusMsg.isNotBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isError) colors.dangerSoft else colors.primarySoft,
                                                border = BorderStroke(1.dp, if (isError) colors.danger.copy(alpha = 0.3f) else colors.primary.copy(alpha = 0.3f)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = if (isError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                                                        contentDescription = null,
                                                        tint = if (isError) colors.danger else colors.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = statusMsg,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = if (isError) colors.danger else colors.accentText,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }
                                        }

                                        // Submit button (Sign In or Create Account)
                                        Button(
                                            onClick = {
                                                if (email.isBlank() || password.isBlank()) {
                                                    statusMsg = "Please enter both email and password."
                                                    isError = true
                                                    return@Button
                                                }
                                                if (isSignUpMode && password != confirmPassword) {
                                                    statusMsg = "Passwords do not match."
                                                    isError = true
                                                    return@Button
                                                }
                                                loading = true
                                                statusMsg = "Processing..."
                                                isError = false
                                                viewModel.authenticateWithEmail(email, password, isSignUpMode) { success, msg ->
                                                    loading = false
                                                    statusMsg = msg
                                                    isError = !success
                                                }
                                            },
                                            enabled = !loading,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = colors.primary,
                                                contentColor = colors.onPrimary
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                                .testTag("firebase_submit_button")
                                        ) {
                                            if (loading) {
                                                CircularProgressIndicator(
                                                    color = colors.onPrimary,
                                                    modifier = Modifier.size(20.dp),
                                                    strokeWidth = 2.dp
                                                )
                                            } else {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = if (isSignUpMode) Icons.Default.PersonAdd else Icons.AutoMirrored.Filled.Login,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = if (isSignUpMode) "Create Account" else "Sign In",
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.titleSmall
                                                    )
                                                }
                                            }
                                        }

                                        // Divider with "OR"
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            HorizontalDivider(modifier = Modifier.weight(1f), color = colors.line)
                                            Text(
                                                text = "  OR  ",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colors.muted
                                            )
                                            HorizontalDivider(modifier = Modifier.weight(1f), color = colors.line)
                                        }

                                        // Continue with Google Button
                                        Surface(
                                            onClick = {
                                                loading = true
                                                statusMsg = "Signing in with Google..."
                                                isError = false
                                                viewModel.continueWithGoogle { success, msg ->
                                                    loading = false
                                                    statusMsg = msg
                                                    isError = !success
                                                }
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            color = colors.surface2,
                                            border = BorderStroke(1.dp, colors.line),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                                .testTag("firebase_google_button")
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxSize(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = colors.primarySoft,
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(
                                                            text = "G",
                                                            fontWeight = FontWeight.Black,
                                                            fontSize = 14.sp,
                                                            color = colors.primary
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = "Continue with Google",
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = colors.text
                                                )
                                            }
                                        }
                                    }

                                    // Forgot Password Dialog
                                    if (showForgotDialog) {
                                        AlertDialog(
                                            onDismissRequest = { showForgotDialog = false },
                                            title = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.LockReset,
                                                        contentDescription = null,
                                                        tint = colors.primary,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "Reset Password",
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = colors.text
                                                    )
                                                }
                                            },
                                            text = {
                                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                    Text(
                                                        text = "Enter your registered email address to receive password reset instructions.",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = colors.muted
                                                    )

                                                    OutlinedTextField(
                                                        value = forgotEmail,
                                                        onValueChange = { forgotEmail = it },
                                                        label = { Text("Email Address", color = colors.muted) },
                                                        singleLine = true,
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                                        shape = RoundedCornerShape(10.dp),
                                                        colors = OutlinedTextFieldDefaults.colors(
                                                            focusedBorderColor = colors.primary,
                                                            unfocusedBorderColor = colors.line,
                                                            focusedTextColor = colors.text,
                                                            unfocusedTextColor = colors.text
                                                        ),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .testTag("forgot_password_email_input")
                                                    )

                                                    if (forgotMsg.isNotBlank()) {
                                                        Surface(
                                                            shape = RoundedCornerShape(8.dp),
                                                            color = if (forgotError) colors.dangerSoft else colors.primarySoft,
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            Text(
                                                                text = forgotMsg,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = if (forgotError) colors.danger else colors.accentText,
                                                                modifier = Modifier.padding(10.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            },
                                            confirmButton = {
                                                Button(
                                                    onClick = {
                                                        if (forgotEmail.isBlank()) {
                                                            forgotMsg = "Please enter your email address."
                                                            forgotError = true
                                                            return@Button
                                                        }
                                                        viewModel.sendPasswordReset(forgotEmail) { success, msg ->
                                                            forgotMsg = msg
                                                            forgotError = !success
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = colors.primary,
                                                        contentColor = colors.onPrimary
                                                    ),
                                                    modifier = Modifier.testTag("forgot_password_submit_button")
                                                ) {
                                                    Text("Send Reset Link", fontWeight = FontWeight.Bold)
                                                }
                                            },
                                            dismissButton = {
                                                TextButton(onClick = { showForgotDialog = false }) {
                                                    Text("Close", color = colors.muted)
                                                }
                                            },
                                            containerColor = colors.surface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // STUDENT FLOW: Department
                WizardStep.STUDENT_DEPARTMENT -> {
                    item {
                        StepHeader("Step 1: Department", "Choose your department. Faculty of Computing is live.")
                    }

                    items(DepartmentsData.ALL_DEPARTMENTS) { dept ->
                        DepartmentItem(
                            dept = dept,
                            isSelected = dept.name == studentDept.name,
                            onClick = {
                                if (dept.isLive) {
                                    studentDept = dept
                                    studentProgram = null
                                    studentSemester = null
                                    studentShift = null
                                    studentSection = null
                                } else {
                                    comingSoonDeptClicked = dept
                                }
                            }
                        )
                    }
                }

                // STUDENT FLOW: Program
                WizardStep.STUDENT_PROGRAM -> {
                    item {
                        StepHeader("Step 2: Program / Course", "Select your degree program")
                    }

                    items(studentDept.programs) { program ->
                        val isSelected = studentProgram?.code == program.code
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) colors.primarySoft else colors.surface,
                            border = BorderStroke(1.2.dp, if (isSelected) colors.primary else colors.line),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { studentProgram = program }
                                .testTag("program_item_${program.code}")
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
                                        text = program.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) colors.primary else colors.text
                                    )
                                    if (program.isFifthSemesterEntry) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "5th Semester Lateral Entry",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.accentText
                                        )
                                    }
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { studentProgram = program },
                                    colors = RadioButtonDefaults.colors(selectedColor = colors.primary)
                                )
                            }
                        }
                    }
                }

                // STUDENT FLOW: Semester & Shift
                WizardStep.STUDENT_SEMESTER_SHIFT -> {
                    item {
                        StepHeader("Step 3: Semester & Shift", "Pick your semester and time shift")
                    }

                    item {
                        Text(
                            text = "Semester",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.text
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val availableSemesters = studentProgram?.defaultSemesters ?: listOf(1, 2, 3, 4, 5, 6, 7, 8)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(availableSemesters) { sem ->
                                val isSelected = studentSemester == sem
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { studentSemester = sem },
                                    label = {
                                        Text(
                                            text = "${sem}${getOrdinal(sem)} Sem",
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = colors.primary,
                                        selectedLabelColor = colors.onPrimary,
                                        containerColor = colors.surface2,
                                        labelColor = colors.text
                                    ),
                                    modifier = Modifier.testTag("semester_chip_$sem")
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Shift / Class Time",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.text
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Regular (First Time)
                            ShiftOptionCard(
                                title = "First Time (Regular - R)",
                                description = "Standard morning/afternoon classes (R)",
                                isSelected = studentShift == "regular",
                                onClick = { studentShift = "regular" },
                                testTag = "shift_regular_option"
                            )

                            // Self Study (Second Time)
                            ShiftOptionCard(
                                title = "Second Time (Self Study - SS)",
                                description = "Afternoon classes (SS1, SS2, etc.)",
                                isSelected = studentShift == "second",
                                onClick = { studentShift = "second" },
                                testTag = "shift_second_option"
                            )
                        }
                    }
                }

                // STUDENT FLOW: Section (Only if Second Time)
                WizardStep.STUDENT_SECTION -> {
                    item {
                        StepHeader("Step 4: Section", "Choose your SS section")
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf(
                                Pair(null, "No Specific Section (SS General)"),
                                Pair("SS1", "Section SS1"),
                                Pair("SS2", "Section SS2")
                            ).forEach { (sec, label) ->
                                val isSelected = studentSection == sec
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) colors.primarySoft else colors.surface,
                                    border = BorderStroke(1.2.dp, if (isSelected) colors.primary else colors.line),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { studentSection = sec }
                                        .testTag("section_option_${sec ?: "none"}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) colors.primary else colors.text
                                        )
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { studentSection = sec },
                                            colors = RadioButtonDefaults.colors(selectedColor = colors.primary)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // TEACHER FLOW: Department
                WizardStep.TEACHER_DEPARTMENT -> {
                    item {
                        StepHeader("Step 1: Department", "Choose your faculty department. Faculty of Computing is live.")
                    }

                    items(DepartmentsData.ALL_DEPARTMENTS) { dept ->
                        DepartmentItem(
                            dept = dept,
                            isSelected = dept.name == teacherDept.name,
                            onClick = {
                                if (dept.isLive) {
                                    teacherDept = dept
                                    selectedTeacher = null
                                } else {
                                    comingSoonDeptClicked = dept
                                }
                            }
                        )
                    }
                }

                // TEACHER FLOW: Select Teacher
                WizardStep.TEACHER_SELECT -> {
                    item {
                        StepHeader("Step 2: Find Your Name", "Search or tap your name to view your timetable")
                    }

                    item {
                        OutlinedTextField(
                            value = teacherSearchQuery,
                            onValueChange = { teacherSearchQuery = it },
                            placeholder = { Text("Search teacher name...", color = colors.muted) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = colors.muted) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("teacher_search_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.primary,
                                unfocusedBorderColor = colors.line,
                                focusedTextColor = colors.text,
                                unfocusedTextColor = colors.text
                            )
                        )
                    }

                    items(filteredTeachers) { teacher ->
                        val isSelected = selectedTeacher?.key == teacher.key
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) colors.primarySoft else colors.surface,
                            border = BorderStroke(1.2.dp, if (isSelected) colors.primary else colors.line),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedTeacher = teacher }
                                .testTag("teacher_item_${teacher.key}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) colors.primary else colors.surface2),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = if (isSelected) colors.onPrimary else colors.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = teacher.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) colors.primary else colors.text
                                        )
                                        Text(
                                            text = "${teacher.lectureCount} lectures scheduled this week",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.muted
                                        )
                                    }
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedTeacher = teacher },
                                    colors = RadioButtonDefaults.colors(selectedColor = colors.primary)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    comingSoonDeptClicked?.let { dept ->
        AlertDialog(
            onDismissRequest = { comingSoonDeptClicked = null },
            title = {
                Text(
                    text = "${dept.name} Coming Soon",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.text
                )
            },
            text = {
                Column {
                    Text(
                        text = "We are adding this department's timetable soon.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.text
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Faculty of Computing is currently active. Get notified as soon as new schedules are published.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { comingSoonDeptClicked = null },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary)
                ) {
                    Text("OK, Understood")
                }
            },
            containerColor = colors.surface
        )
    }
}

@Composable
fun StepHeader(title: String, subtitle: String) {
    val colors = AppTheme.colors
    Column {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = colors.text
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.muted
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun DepartmentItem(
    dept: Department,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) colors.primarySoft else colors.surface,
        border = BorderStroke(1.2.dp, if (isSelected) colors.primary else colors.line),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag("department_item_${dept.name}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dept.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) colors.primary else colors.text,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (dept.isLive) {
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = colors.successSoft,
                            border = BorderStroke(1.dp, colors.success)
                        ) {
                            Text(
                                text = "LIVE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.success,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = colors.accentSoft,
                            border = BorderStroke(1.dp, colors.accent)
                        ) {
                            Text(
                                text = "Coming soon",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.accentText,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (dept.isLive) "${dept.programs.size} degree programs active" else "Schedules being imported",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted
                )
            }
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = colors.primary)
            )
        }
    }
}

@Composable
fun ShiftOptionCard(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val colors = AppTheme.colors
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) colors.primarySoft else colors.surface,
        border = BorderStroke(1.2.dp, if (isSelected) colors.primary else colors.line),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag(testTag)
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
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) colors.primary else colors.text
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted
                )
            }
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = colors.primary)
            )
        }
    }
}
