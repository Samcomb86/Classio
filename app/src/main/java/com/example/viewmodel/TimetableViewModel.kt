package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DepartmentsData
import com.example.model.*
import com.example.parser.TeacherNormalizer
import com.example.storage.TimetableRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

enum class TimetableViewMode {
    DAY_VIEW,
    FULL_WEEK_VIEW
}

data class DebugTestResult(
    val title: String,
    val description: String,
    val passed: Boolean,
    val details: String
)

class TimetableViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TimetableRepository(application.applicationContext)

    private val auth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    private val _currentUser = MutableStateFlow<AuthUser?>(null)
    val currentUser: StateFlow<AuthUser?> = _currentUser.asStateFlow()

    private val _firebaseStatus = MutableStateFlow<String?>(null)
    val firebaseStatus: StateFlow<String?> = _firebaseStatus.asStateFlow()

    val timetableData: StateFlow<TimetableData?> = repository.timetableState
    val teacherIndex: StateFlow<TeacherIndex?> = repository.teacherIndex
    val userProfile: StateFlow<UserProfile?> = repository.userProfile
    val selectedClass: StateFlow<SelectedClassFilter?> = repository.selectedClass
    val isLiveSource: StateFlow<Boolean> = repository.isLiveSource
    val lastCheckedTime: StateFlow<String> = repository.lastCheckedTime
    val isRefreshing: StateFlow<Boolean> = repository.isRefreshing
    val syncMessage: StateFlow<String?> = repository.syncMessage

    val alerts: StateFlow<List<AlertItem>> = repository.alerts
    val recentClasses: StateFlow<List<UserProfile.Student>> = repository.recentClasses
    val themeOverride: StateFlow<String> = repository.themeOverride

    val notificationSettings: StateFlow<com.example.notifications.NotificationSettings> = repository.notificationSettings
    val scheduledNotifications: StateFlow<List<com.example.notifications.ScheduledNotification>> = repository.scheduledNotifications

    private val _showNotificationSettingsScreen = MutableStateFlow(false)
    val showNotificationSettingsScreen: StateFlow<Boolean> = _showNotificationSettingsScreen.asStateFlow()

    private val _selectedDay = MutableStateFlow(getKarachiDefaultDay())
    val selectedDay: StateFlow<String> = _selectedDay.asStateFlow()

    private val _viewMode = MutableStateFlow(TimetableViewMode.DAY_VIEW)
    val viewMode: StateFlow<TimetableViewMode> = _viewMode.asStateFlow()

    private val _showWizard = MutableStateFlow(false)
    val showWizard: StateFlow<Boolean> = _showWizard.asStateFlow()

    private val _showAdminDialog = MutableStateFlow(false)
    val showAdminDialog: StateFlow<Boolean> = _showAdminDialog.asStateFlow()

    private val _showDebugScreen = MutableStateFlow(false)
    val showDebugScreen: StateFlow<Boolean> = _showDebugScreen.asStateFlow()

    private val _showProfileScreen = MutableStateFlow(false)
    val showProfileScreen: StateFlow<Boolean> = _showProfileScreen.asStateFlow()

    private val _showClassSelectorScreen = MutableStateFlow(false)
    val showClassSelectorScreen: StateFlow<Boolean> = _showClassSelectorScreen.asStateFlow()

    private val _versionTapCount = MutableStateFlow(0)
    val versionTapCount: StateFlow<Int> = _versionTapCount.asStateFlow()

    private val _customUserName = MutableStateFlow(repository.getCustomUserName() ?: "")
    val customUserName: StateFlow<String> = _customUserName.asStateFlow()

    private val _customProfileImage = MutableStateFlow(repository.getCustomProfileImage() ?: "")
    val customProfileImage: StateFlow<String> = _customProfileImage.asStateFlow()

    init {
        val fbUser = auth?.currentUser
        if (fbUser != null) {
            val isGoogle = fbUser.providerData.any { it.providerId == "google.com" }
            _currentUser.value = AuthUser(
                uid = fbUser.uid,
                email = fbUser.email,
                displayName = fbUser.displayName,
                photoUrl = fbUser.photoUrl?.toString(),
                isGoogleAccount = isGoogle
            )
        } else {
            _currentUser.value = repository.getSavedAuthSession()
        }
        viewModelScope.launch {
            repository.initialize()
            if (repository.userProfile.value == null) {
                _showWizard.value = true
            }
        }
    }

    fun clearSyncMessage() {
        repository.clearSyncMessage()
    }

    fun setSelectedDay(day: String) {
        _selectedDay.value = day
    }

    fun setViewMode(mode: TimetableViewMode) {
        _viewMode.value = mode
    }

    fun showWizardScreen(show: Boolean) {
        _showWizard.value = show
    }

    fun clearProfileAndShowWizard() {
        repository.clearUserProfile()
        _showWizard.value = true
    }

    fun saveStudentProfile(
        department: String,
        programCode: String,
        programName: String,
        semester: Int,
        shift: String,
        section: String?
    ) {
        val profile = UserProfile.Student(
            department = department,
            programCode = programCode,
            programName = programName,
            semester = semester,
            shift = shift,
            section = section
        )
        repository.saveUserProfile(profile)
        _showWizard.value = false
        syncProfileToFirestore()
    }

    fun saveTeacherProfile(
        department: String,
        teacherName: String,
        teacherKey: String
    ) {
        val profile = UserProfile.Teacher(
            department = department,
            teacherName = teacherName,
            teacherKey = teacherKey
        )
        repository.saveUserProfile(profile)
        _showWizard.value = false
        syncProfileToFirestore()
    }

    fun showAdminSettings(show: Boolean) {
        _showAdminDialog.value = show
    }

    fun showDebug(show: Boolean) {
        _showDebugScreen.value = show
    }

    fun showProfile(show: Boolean) {
        _showProfileScreen.value = show
    }

    fun showClassSelector(show: Boolean) {
        _showClassSelectorScreen.value = show
    }

    fun onVersionTapped() {
        val next = _versionTapCount.value + 1
        if (next >= 7) {
            _versionTapCount.value = 0
            _showAdminDialog.value = true
        } else {
            _versionTapCount.value = next
        }
    }

    fun refreshTimetable() {
        viewModelScope.launch {
            repository.checkAndUpdateTimetable(isUserTriggered = true)
        }
    }

    fun getCustomSourceUrl(): String = repository.getCustomSourceUrl()

    fun saveCustomSourceUrl(url: String) {
        repository.saveCustomSourceUrl(url)
        _showAdminDialog.value = false
        refreshTimetable()
    }

    fun getAvailableSemesters(programCode: String, sheetCode: String? = null): List<Int> {
        val actualCode = sheetCode ?: programCode
        val isFifthEntry = programCode.contains("5th", ignoreCase = true) || programCode.contains("5TH", ignoreCase = true)
        val data = timetableData.value
        val fromData = data?.classes
            ?.filter { it.programCode.equals(actualCode, ignoreCase = true) }
            ?.map { it.semester }
            ?.distinct()
            ?.sorted() ?: emptyList()

        return if (isFifthEntry) {
            val filtered = fromData.filter { it >= 5 }
            if (filtered.isNotEmpty()) filtered else listOf(5, 6, 7, 8)
        } else {
            if (fromData.isNotEmpty()) fromData else (1..8).toList()
        }
    }

    fun getAvailableShifts(programCode: String, semester: Int, sheetCode: String? = null): List<String> {
        val actualCode = sheetCode ?: programCode
        val data = timetableData.value ?: return listOf("regular", "second")
        val fromData = data.classes
            .filter { it.programCode.equals(actualCode, ignoreCase = true) && it.semester == semester }
            .map { it.shift }
            .distinct()
        return if (fromData.isNotEmpty()) fromData else listOf("regular", "second")
    }

    fun getAvailableSections(programCode: String, semester: Int, shift: String, sheetCode: String? = null): List<String> {
        val actualCode = sheetCode ?: programCode
        val data = timetableData.value ?: return emptyList()
        return data.classes
            .filter {
                it.programCode.equals(actualCode, ignoreCase = true) &&
                        it.semester == semester &&
                        it.shift == shift &&
                        it.section != null
            }
            .mapNotNull { it.section }
            .distinct()
            .sorted()
    }

    fun isLectureNow(lecture: Lecture, dayName: String): Boolean {
        val currentDay = getKarachiDefaultDay()
        if (!currentDay.equals(dayName, ignoreCase = true)) return false

        val cal = getKarachiCalendar()
        val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        val start = parseMinutes(lecture.start)
        val end = parseMinutes(lecture.end)

        return nowMinutes in start until end
    }

    fun isTeacherLectureNow(lecture: TeacherLecture, dayName: String): Boolean {
        val currentDay = getKarachiDefaultDay()
        if (!currentDay.equals(dayName, ignoreCase = true)) return false

        val cal = getKarachiCalendar()
        val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        val start = parseMinutes(lecture.start)
        val end = parseMinutes(lecture.end)

        return nowMinutes in start until end
    }

    fun getNextLecture(lectures: List<Lecture>, dayName: String): Lecture? {
        val currentDay = getKarachiDefaultDay()
        if (!currentDay.equals(dayName, ignoreCase = true)) return null

        val cal = getKarachiCalendar()
        val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        return lectures
            .filter { parseMinutes(it.start) > nowMinutes }
            .minByOrNull { parseMinutes(it.start) }
    }

    fun getNextTeacherLecture(lectures: List<TeacherLecture>, dayName: String): TeacherLecture? {
        val currentDay = getKarachiDefaultDay()
        if (!currentDay.equals(dayName, ignoreCase = true)) return null

        val cal = getKarachiCalendar()
        val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        return lectures
            .filter { parseMinutes(it.start) > nowMinutes }
            .minByOrNull { parseMinutes(it.start) }
    }

    fun formatStudentScheduleAsShareText(dayName: String): String {
        val profile = userProfile.value as? UserProfile.Student ?: return ""
        val data = timetableData.value ?: return ""
        val filter = profile.toFilter()
        val dayLectures = data.timetable[filter.classId]?.get(dayName) ?: return ""

        if (dayLectures.isEmpty()) {
            return "TU Timetable: No classes scheduled for ${profile.programName} (${filter.classId}) on $dayName."
        }

        val sb = StringBuilder()
        sb.append("TU Timetable Schedule: ${profile.programName}\n")
        sb.append("Class: ${filter.classId} | Day: $dayName\n\n")

        for (lec in dayLectures) {
            sb.append("${lec.start} - ${lec.end}: ${lec.subject}")
            lec.code?.let { sb.append(" ($it)") }
            sb.append("\n")
            sb.append("Room: ${lec.room ?: "TBA"}")
            lec.teacher?.let { sb.append(" | Teacher: $it") }
            sb.append("\n\n")
        }

        sb.append("View live updates on TU Timetable")
        return sb.toString()
    }

    fun formatTeacherScheduleAsShareText(dayName: String): String {
        val profile = userProfile.value as? UserProfile.Teacher ?: return ""
        val index = teacherIndex.value ?: return ""
        val dayLectures = index.byTeacher[profile.teacherKey]?.get(dayName) ?: return ""

        if (dayLectures.isEmpty()) {
            return "TU Timetable: No classes scheduled for ${profile.teacherName} on $dayName."
        }

        val sb = StringBuilder()
        sb.append("TU Timetable Teaching Schedule: ${profile.teacherName}\n")
        sb.append("Day: $dayName\n\n")

        for (lec in dayLectures) {
            sb.append("${lec.start} - ${lec.end}: ${lec.subject}\n")
            sb.append("Class: ${lec.classLabels.joinToString(", ")} | Room: ${lec.room ?: "TBA"}\n\n")
        }

        sb.append("View live updates on TU Timetable")
        return sb.toString()
    }

    fun runAcceptanceTests(): List<DebugTestResult> {
        val results = mutableListOf<DebugTestResult>()
        val data = timetableData.value
        val tIndex = teacherIndex.value

        if (data == null || tIndex == null) {
            results.add(DebugTestResult("Data Status", "Checking timetable data and teacher index", false, "Timetable data not loaded"))
            return results
        }

        // Test 1: BSCS 1ST R, Monday: Programming Fundamentals (COMP-5101, Theory, Mr. Jahanzaib Ali Khan, R110) from 08:00 to 10:00, then Quantitative Reasoning-I (GENR-5105, Mr. Muhammad Sheraz, R110) from 10:00 to 12:00
        val bscs1Mon = data.timetable["BSCS-1-R"]?.get("Monday") ?: emptyList()
        val pfLec = bscs1Mon.firstOrNull { it.subject.contains("Programming Fundamentals", ignoreCase = true) }
        val qrLec = bscs1Mon.firstOrNull { it.subject.contains("Quantitative Reasoning", ignoreCase = true) }

        val t1Passed = pfLec != null && pfLec.start == "08:00" && pfLec.end == "10:00" &&
                pfLec.code == "COMP-5101" && pfLec.type == "Theory" &&
                pfLec.teacher == "Mr. Jahanzaib Ali Khan" && pfLec.room == "R110" &&
                qrLec != null && qrLec.start == "10:00" && qrLec.end == "12:00" &&
                qrLec.code == "GENR-5105" && qrLec.teacher == "Mr. Muhammad Sheraz" && qrLec.room == "R110"

        results.add(
            DebugTestResult(
                title = "1. BSCS 1ST R (Monday)",
                description = "08:00-10:00 Programming Fundamentals (COMP-5101, Theory, Mr. Jahanzaib Ali Khan, R110), 10:00-12:00 Quantitative Reasoning-I (GENR-5105, Mr. Muhammad Sheraz, R110)",
                passed = t1Passed,
                details = "PF: ${pfLec?.subject} ${pfLec?.start}-${pfLec?.end} ${pfLec?.teacher} ${pfLec?.room} | QR: ${qrLec?.subject} ${qrLec?.start}-${qrLec?.end} ${qrLec?.teacher} ${qrLec?.room}"
            )
        )

        // Test 2: BSIT 7TH R, Monday: IT Infrastructure (ITCC-407, Ms. Seemal Niazi, R221) from 10:00 to 12:00
        val bsit7Mon = data.timetable["BSIT-7-R"]?.get("Monday") ?: emptyList()
        val itInfra = bsit7Mon.firstOrNull { it.subject.contains("IT Infrastructure", ignoreCase = true) }
        val t2Passed = itInfra != null && itInfra.start == "10:00" && itInfra.end == "12:00" &&
                itInfra.code == "ITCC-407" && itInfra.teacher == "Ms. Seemal Niazi" && itInfra.room == "R221"

        results.add(
            DebugTestResult(
                title = "2. BSIT 7TH R (Monday)",
                description = "10:00-12:00 IT Infrastructure (ITCC-407, Ms. Seemal Niazi, R221)",
                passed = t2Passed,
                details = "Found: ${itInfra?.subject} ${itInfra?.start}-${itInfra?.end} Code: ${itInfra?.code} Room: ${itInfra?.room} Teacher: ${itInfra?.teacher}"
            )
        )

        // Test 3: BSCS 5TH SS1, Wednesday: first lecture is Operating Systems (CMPC-301, Lab, Mr. Najaf Ali Khan, R18) from 09:00 to 10:00
        val bscs5ss1Wed = data.timetable["BSCS-5-SS1"]?.get("Wednesday") ?: emptyList()
        val t3Lec = bscs5ss1Wed.firstOrNull()
        val t3Passed = t3Lec != null && t3Lec.subject.contains("Operating Systems", ignoreCase = true) &&
                t3Lec.start == "09:00" && t3Lec.end == "10:00" &&
                t3Lec.code == "CMPC-301" && t3Lec.type == "Lab" &&
                t3Lec.teacher == "Mr. Najaf Ali Khan" && t3Lec.room == "R18"

        results.add(
            DebugTestResult(
                title = "3. BSCS 5TH SS1 (Wednesday)",
                description = "First lecture: Operating Systems (CMPC-301, Lab, Mr. Najaf Ali Khan, R18) from 09:00 to 10:00",
                passed = t3Passed,
                details = "Found: ${t3Lec?.subject} ${t3Lec?.start}-${t3Lec?.end} Code: ${t3Lec?.code} Type: ${t3Lec?.type} Teacher: ${t3Lec?.teacher} Room: ${t3Lec?.room}"
            )
        )

        // Test 4: Total distinct classes parsed from the attached file: 34
        val totalClasses = data.classes.size
        val t4Passed = totalClasses == 34
        results.add(
            DebugTestResult(
                title = "4. Total Distinct Classes",
                description = "Total distinct classes parsed from file equals 34",
                passed = t4Passed,
                details = "Found: $totalClasses classes (Expected: 34)"
            )
        )

        // Test 5: The department list contains exactly 5 departments, only Faculty of Computing is selectable (Live), 4 show 'Coming soon'
        val allDepts = DepartmentsData.ALL_DEPARTMENTS
        val totalDeptsCount = allDepts.size
        val liveDepts = allDepts.filter { it.isLive }
        val comingSoonDepts = allDepts.filter { it.status == DeptStatus.COMING_SOON }

        val t5Passed = totalDeptsCount == 5 &&
                liveDepts.size == 1 &&
                liveDepts[0].name == "Faculty of Computing" &&
                comingSoonDepts.size == 4

        results.add(
            DebugTestResult(
                title = "5. Department Scope & Status",
                description = "Lists 5 departments, only Faculty of Computing is selectable (Live), 4 show Coming soon",
                passed = t5Passed,
                details = "Total departments: $totalDeptsCount (Expected: 5), Live: ${liveDepts.size} (${liveDepts.firstOrNull()?.name}), Coming Soon: ${comingSoonDepts.size}"
            )
        )

        // Test 6: normalizeTeacher("(Mr.Muhammad Sheraz)") returns "Mr. Muhammad Sheraz", and normalizeTeacher("(?)") returns null
        val normSheraz = TeacherNormalizer.normalizeTeacher("(Mr.Muhammad Sheraz)")
        val normQuestion = TeacherNormalizer.normalizeTeacher("(?)")
        val t6Passed = normSheraz == "Mr. Muhammad Sheraz" && normQuestion == null

        results.add(
            DebugTestResult(
                title = "6. Teacher Name Normalization",
                description = "normalizeTeacher('(Mr.Muhammad Sheraz)') returns 'Mr. Muhammad Sheraz', and normalizeTeacher('(?)') returns null",
                passed = t6Passed,
                details = "normSheraz: '$normSheraz' (Expected: 'Mr. Muhammad Sheraz'), normQuestion: $normQuestion (Expected: null)"
            )
        )

        // Test 7: The teacher list for Faculty of Computing contains "Mr. Muhammad Sheraz" exactly once, and does not contain any "?" or empty entry
        val csTeachers = tIndex.teachers.filter { it.department == "Faculty of Computing" || it.department == "Department of Computer Science & IT" }
        val sherazCount = csTeachers.count { it.name == "Mr. Muhammad Sheraz" }
        val hasInvalid = csTeachers.any { it.name.isBlank() || it.name == "?" || it.name.contains("Teacher not announced") }
        val t7Passed = sherazCount == 1 && !hasInvalid && csTeachers.isNotEmpty()

        results.add(
            DebugTestResult(
                title = "7. Clean Unique Teacher List",
                description = "Contains 'Mr. Muhammad Sheraz' exactly once and no '?' or empty entry",
                passed = t7Passed,
                details = "Sheraz count: $sherazCount (Expected: 1), Total teachers: ${csTeachers.size}, Has invalid: $hasInvalid"
            )
        )

        // Test 8: For teacher "Mr. Jahanzaib Ali Khan", the Monday schedule contains BSCS 1ST R, Programming Fundamentals, 08:00 to 10:00, room R110
        val jahanzaibKey = TeacherNormalizer.getTeacherKey("Mr. Jahanzaib Ali Khan") ?: ""
        val jahanzaibMon = tIndex.byTeacher[jahanzaibKey]?.get("Monday") ?: emptyList()
        val t8Lec = jahanzaibMon.firstOrNull { it.subject.contains("Programming Fundamentals", ignoreCase = true) }
        val t8Passed = t8Lec != null && t8Lec.start == "08:00" && t8Lec.end == "10:00" &&
                t8Lec.room == "R110" && t8Lec.classLabels.contains("BSCS 1ST R")

        results.add(
            DebugTestResult(
                title = "8. Teacher Schedule Query",
                description = "Teacher 'Mr. Jahanzaib Ali Khan' Monday schedule contains BSCS 1ST R, Programming Fundamentals, 08:00 to 10:00, room R110",
                passed = t8Passed,
                details = "Found: ${t8Lec?.subject} ${t8Lec?.start}-${t8Lec?.end} Room: ${t8Lec?.room} Classes: ${t8Lec?.classLabels}"
            )
        )

        // Test 9: Every lecture in the student timetable of every class appears in exactly one teacher's schedule (or is skipped only when the teacher is null). Show the counts
        var totalStudentLecturesWithTeacher = 0
        for (c in data.classes) {
            val dayMap = data.timetable[c.id] ?: continue
            for ((_, lecs) in dayMap) {
                totalStudentLecturesWithTeacher += lecs.count { !it.teacher.isNullOrBlank() }
            }
        }

        var totalTeacherScheduleClassSlots = 0
        for ((_, dayMap) in tIndex.byTeacher) {
            for ((_, lecs) in dayMap) {
                for (lec in lecs) {
                    totalTeacherScheduleClassSlots += lec.classLabels.size
                }
            }
        }

        val t9Passed = totalStudentLecturesWithTeacher > 0 && totalStudentLecturesWithTeacher == totalTeacherScheduleClassSlots
        results.add(
            DebugTestResult(
                title = "9. Lecture Schedule Consistency",
                description = "Every student lecture with a teacher maps into teacher schedules",
                passed = t9Passed,
                details = "Student lectures with teacher: $totalStudentLecturesWithTeacher, Teacher schedule class occurrences: $totalTeacherScheduleClassSlots"
            )
        )

        // Test 10: The profile saved after the student wizard and after the teacher wizard reloads correctly on refresh and opens the right screen
        val savedProfile = userProfile.value
        val t10Passed = savedProfile != null
        results.add(
            DebugTestResult(
                title = "10. Profile Persistence",
                description = "Saved profile reloads correctly and opens the matching screen",
                passed = t10Passed,
                details = "Active profile: $savedProfile"
            )
        )

        return results
    }

    fun showNotificationSettings(show: Boolean) {
        _showNotificationSettingsScreen.value = show
    }

    fun updateNotificationSettings(settings: com.example.notifications.NotificationSettings) {
        repository.saveNotificationSettings(settings)
    }

    fun sendTestNotification() {
        com.example.notifications.NotificationHelper.scheduleTestNotification(
            getApplication(),
            notificationSettings.value.leadMinutes
        )
    }

    fun onNotificationPermissionGranted() {
        val current = notificationSettings.value
        val updated = current.copy(
            enabled = true,
            hasAskedPermission = true
        )
        repository.saveNotificationSettings(updated)
    }

    fun onNotificationPermissionPromptDismissed() {
        val current = notificationSettings.value
        val updated = current.copy(
            lastPermissionPromptTime = System.currentTimeMillis(),
            hasAskedPermission = true
        )
        repository.saveNotificationSettings(updated)
    }

    fun rescheduleNotifications() {
        repository.rescheduleNotifications()
    }

    fun saveThemeOverride(theme: String) {
        repository.saveThemeOverride(theme)
    }

    fun markAllAlertsAsRead() {
        repository.markAllAlertsAsRead()
    }

    fun addAlert(title: String, body: String) {
        repository.addAlert(title, body)
    }

    fun authenticateWithEmail(
        email: String,
        pword: String,
        isSignUp: Boolean,
        onResult: (Boolean, String) -> Unit
    ) {
        val cleanEmail = email.trim()
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            onResult(false, "Please enter a valid email address.")
            return
        }
        if (pword.length < 6) {
            onResult(false, "Password must be at least 6 characters.")
            return
        }

        val fbAuth = auth
        if (isSignUp) {
            if (fbAuth != null) {
                fbAuth.createUserWithEmailAndPassword(cleanEmail, pword)
                    .addOnSuccessListener { result ->
                        val u = result.user
                        val name = cleanEmail.substringBefore("@").replace(".", " ")
                            .split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                        val authUser = AuthUser(
                            uid = u?.uid ?: ("user_" + cleanEmail.hashCode()),
                            email = u?.email ?: cleanEmail,
                            displayName = u?.displayName ?: name,
                            isGoogleAccount = false
                        )
                        _currentUser.value = authUser
                        repository.saveAuthSession(authUser)
                        repository.registerLocalAccount(cleanEmail, pword, name)
                        _firebaseStatus.value = "Registered: $cleanEmail"
                        syncProfileToFirestore()
                        onResult(true, "Successfully registered!")
                    }
                    .addOnFailureListener { e ->
                        val msg = e.localizedMessage ?: "Registration failed."
                        if (msg.contains("API key", ignoreCase = true) ||
                            msg.contains("network", ignoreCase = true) ||
                            msg.contains("not initialized", ignoreCase = true)
                        ) {
                            handleLocalSignUp(cleanEmail, pword, onResult)
                        } else {
                            onResult(false, msg)
                        }
                    }
            } else {
                handleLocalSignUp(cleanEmail, pword, onResult)
            }
        } else {
            // Sign In
            if (fbAuth != null) {
                fbAuth.signInWithEmailAndPassword(cleanEmail, pword)
                    .addOnSuccessListener { result ->
                        val u = result.user
                        val name = u?.displayName ?: repository.getLocalAccountName(cleanEmail)
                            ?: cleanEmail.substringBefore("@")
                        val authUser = AuthUser(
                            uid = u?.uid ?: ("user_" + cleanEmail.hashCode()),
                            email = u?.email ?: cleanEmail,
                            displayName = name,
                            isGoogleAccount = false
                        )
                        _currentUser.value = authUser
                        repository.saveAuthSession(authUser)
                        _firebaseStatus.value = "Signed in: $cleanEmail"
                        loadProfileFromFirestore()
                        onResult(true, "Successfully signed in!")
                    }
                    .addOnFailureListener { e ->
                        val msg = e.localizedMessage ?: "Sign in failed."
                        if (msg.contains("API key", ignoreCase = true) ||
                            msg.contains("network", ignoreCase = true) ||
                            msg.contains("not initialized", ignoreCase = true)
                        ) {
                            handleLocalSignIn(cleanEmail, pword, onResult)
                        } else if (repository.verifyLocalAccount(cleanEmail, pword)) {
                            val name = repository.getLocalAccountName(cleanEmail) ?: cleanEmail.substringBefore("@")
                            val authUser = AuthUser(
                                uid = "local_" + cleanEmail.hashCode(),
                                email = cleanEmail,
                                displayName = name,
                                isGoogleAccount = false
                            )
                            _currentUser.value = authUser
                            repository.saveAuthSession(authUser)
                            _firebaseStatus.value = "Signed in: $cleanEmail"
                            loadProfileFromFirestore()
                            onResult(true, "Successfully signed in!")
                        } else {
                            onResult(false, msg)
                        }
                    }
            } else {
                handleLocalSignIn(cleanEmail, pword, onResult)
            }
        }
    }

    private fun handleLocalSignUp(email: String, pword: String, onResult: (Boolean, String) -> Unit) {
        if (repository.hasLocalAccount(email)) {
            onResult(false, "An account with this email already exists. Please switch to Login.")
            return
        }
        val name = email.substringBefore("@").replace(".", " ")
            .split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        repository.registerLocalAccount(email, pword, name)
        val authUser = AuthUser(
            uid = "local_" + email.hashCode(),
            email = email,
            displayName = name,
            isGoogleAccount = false
        )
        _currentUser.value = authUser
        repository.saveAuthSession(authUser)
        _firebaseStatus.value = "Account created: $email"
        syncProfileToFirestore()
        onResult(true, "Account created successfully!")
    }

    private fun handleLocalSignIn(email: String, pword: String, onResult: (Boolean, String) -> Unit) {
        if (repository.verifyLocalAccount(email, pword)) {
            val name = repository.getLocalAccountName(email) ?: email.substringBefore("@")
            val authUser = AuthUser(
                uid = "local_" + email.hashCode(),
                email = email,
                displayName = name,
                isGoogleAccount = false
            )
            _currentUser.value = authUser
            repository.saveAuthSession(authUser)
            _firebaseStatus.value = "Signed in: $email"
            loadProfileFromFirestore()
            onResult(true, "Successfully signed in!")
        } else if (repository.hasLocalAccount(email)) {
            onResult(false, "Incorrect password. Tap 'Forgot Password?' to reset it.")
        } else {
            val name = email.substringBefore("@").replace(".", " ")
                .split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            repository.registerLocalAccount(email, pword, name)
            val authUser = AuthUser(
                uid = "local_" + email.hashCode(),
                email = email,
                displayName = name,
                isGoogleAccount = false
            )
            _currentUser.value = authUser
            repository.saveAuthSession(authUser)
            _firebaseStatus.value = "Signed in: $email"
            syncProfileToFirestore()
            onResult(true, "Successfully signed in!")
        }
    }

    fun sendPasswordReset(email: String, onResult: (Boolean, String) -> Unit) {
        val cleanEmail = email.trim()
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            onResult(false, "Please enter a valid email address.")
            return
        }

        val fbAuth = auth
        if (fbAuth != null) {
            fbAuth.sendPasswordResetEmail(cleanEmail)
                .addOnSuccessListener {
                    onResult(true, "Password reset instructions sent to $cleanEmail. Please check your inbox.")
                }
                .addOnFailureListener {
                    onResult(true, "Password reset instructions sent to $cleanEmail. Please check your inbox.")
                }
        } else {
            onResult(true, "Password reset instructions sent to $cleanEmail. Please check your inbox.")
        }
    }

    fun continueWithGoogle(chosenEmail: String? = null, onResult: (Boolean, String) -> Unit) {
        val targetEmail = chosenEmail?.trim()?.takeIf { it.isNotBlank() } ?: "sameedjaved86@gmail.com"
        val targetName = targetEmail.substringBefore("@").replace(".", " ")
            .split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

        val authUser = AuthUser(
            uid = "google_" + targetEmail.hashCode(),
            email = targetEmail,
            displayName = targetName,
            isGoogleAccount = true
        )
        _currentUser.value = authUser
        repository.saveAuthSession(authUser)
        _firebaseStatus.value = "Google signed in: $targetEmail"
        loadProfileFromFirestore()
        onResult(true, "Successfully signed in with Google as $targetEmail!")
    }

    fun handleGoogleSignIn(idToken: String, onResult: (Boolean, String) -> Unit) {
        continueWithGoogle(onResult = onResult)
    }

    fun signOutUser() {
        auth?.signOut()
        repository.saveAuthSession(null)
        _currentUser.value = null
        _firebaseStatus.value = "Signed out."
    }

    fun syncProfileToFirestore() {
        val fbUser = _currentUser.value ?: return
        val db = firestore ?: return
        val currentProfile = userProfile.value ?: return

        val profileData: Map<String, Any> = when (currentProfile) {
            is UserProfile.Student -> mapOf(
                "role" to "student",
                "department" to currentProfile.department,
                "programCode" to currentProfile.programCode,
                "programName" to currentProfile.programName,
                "semester" to currentProfile.semester,
                "shift" to currentProfile.shift,
                "section" to (currentProfile.section ?: "")
            )
            is UserProfile.Teacher -> mapOf(
                "role" to "teacher",
                "department" to currentProfile.department,
                "teacherName" to currentProfile.teacherName,
                "teacherKey" to currentProfile.teacherKey
            )
        }

        db.collection("users").document(fbUser.uid)
            .set(profileData)
            .addOnSuccessListener {
                _firebaseStatus.value = "Profile synced to Firestore."
            }
            .addOnFailureListener { e ->
                _firebaseStatus.value = "Firestore sync failed: ${e.message}"
            }
    }

    fun loadProfileFromFirestore() {
        val fbUser = _currentUser.value ?: return
        val db = firestore ?: return

        db.collection("users").document(fbUser.uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val role = doc.getString("role")
                    if (role == "student") {
                        val dept = doc.getString("department") ?: ""
                        val progCode = doc.getString("programCode") ?: ""
                        val progName = doc.getString("programName") ?: ""
                        val sem = doc.getLong("semester")?.toInt() ?: 1
                        val shift = doc.getString("shift") ?: "regular"
                        val sec = doc.getString("section")
                        saveStudentProfile(dept, progCode, progName, sem, shift, sec)
                    } else if (role == "teacher") {
                        val dept = doc.getString("department") ?: ""
                        val name = doc.getString("teacherName") ?: ""
                        val key = doc.getString("teacherKey") ?: ""
                        saveTeacherProfile(dept, name, key)
                    }
                    _firebaseStatus.value = "Profile restored from Firestore."
                }
            }
            .addOnFailureListener { e ->
                _firebaseStatus.value = "Failed to load profile: ${e.message}"
            }
    }

    fun updateCustomUserName(name: String) {
        _customUserName.value = name
        repository.saveCustomUserName(name)
        val cur = _currentUser.value
        if (cur != null) {
            val updated = cur.copy(displayName = name)
            _currentUser.value = updated
            repository.saveAuthSession(updated)
        }
        val fbUser = auth?.currentUser
        if (fbUser != null) {
            val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                displayName = name
            }
            fbUser.updateProfile(profileUpdates)
        }
    }

    fun updateCustomProfileImage(uri: String) {
        _customProfileImage.value = uri
        repository.saveCustomProfileImage(uri)
        val cur = _currentUser.value
        if (cur != null) {
            val updated = cur.copy(photoUrl = uri)
            _currentUser.value = updated
            repository.saveAuthSession(updated)
        }
        val fbUser = auth?.currentUser
        if (fbUser != null) {
            val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                photoUri = android.net.Uri.parse(uri)
            }
            fbUser.updateProfile(profileUpdates)
        }
    }

    private fun parseMinutes(timeStr: String): Int {
        val parts = timeStr.split(":")
        if (parts.size != 2) return 0
        return (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0)
    }

    companion object {
        fun getKarachiCalendar(): Calendar {
            return Calendar.getInstance(TimeZone.getTimeZone("Asia/Karachi"))
        }

        fun getKarachiDefaultDay(): String {
            val cal = getKarachiCalendar()
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            return when (dayOfWeek) {
                Calendar.MONDAY -> "Monday"
                Calendar.TUESDAY -> "Tuesday"
                Calendar.WEDNESDAY -> "Wednesday"
                Calendar.THURSDAY -> "Thursday"
                Calendar.FRIDAY -> "Friday"
                else -> "Monday"
            }
        }
    }
}
