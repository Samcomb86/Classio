package com.example.storage

import android.content.Context
import android.content.SharedPreferences
import com.example.config.AppConfig
import com.example.model.*
import com.example.notifications.NotificationHelper
import com.example.notifications.NotificationScheduler
import com.example.notifications.NotificationSettings
import com.example.notifications.ScheduledNotification
import com.example.parser.TeacherNormalizer
import com.example.parser.TimetableParser
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.InputStreamReader
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class TimetableRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("tu_timetable_prefs", Context.MODE_PRIVATE)

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val dataAdapter = moshi.adapter(TimetableData::class.java)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val _timetableState = MutableStateFlow<TimetableData?>(null)
    val timetableState: StateFlow<TimetableData?> = _timetableState.asStateFlow()

    private val _teacherIndex = MutableStateFlow<TeacherIndex?>(null)
    val teacherIndex: StateFlow<TeacherIndex?> = _teacherIndex.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _selectedClass = MutableStateFlow<SelectedClassFilter?>(null)
    val selectedClass: StateFlow<SelectedClassFilter?> = _selectedClass.asStateFlow()

    private val _isLiveSource = MutableStateFlow(false)
    val isLiveSource: StateFlow<Boolean> = _isLiveSource.asStateFlow()

    private val _lastCheckedTime = MutableStateFlow<String>("")
    val lastCheckedTime: StateFlow<String> = _lastCheckedTime.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private val _notificationSettings = MutableStateFlow(NotificationSettings())
    val notificationSettings: StateFlow<NotificationSettings> = _notificationSettings.asStateFlow()

    private val _scheduledNotifications = MutableStateFlow<List<ScheduledNotification>>(emptyList())
    val scheduledNotifications: StateFlow<List<ScheduledNotification>> = _scheduledNotifications.asStateFlow()

    private val alertsListAdapter = moshi.adapter<List<AlertItem>>(
        com.squareup.moshi.Types.newParameterizedType(List::class.java, AlertItem::class.java)
    )

    private val studentListAdapter = moshi.adapter<List<UserProfile.Student>>(
        com.squareup.moshi.Types.newParameterizedType(List::class.java, UserProfile.Student::class.java)
    )

    private val _alerts = MutableStateFlow<List<AlertItem>>(emptyList())
    val alerts: StateFlow<List<AlertItem>> = _alerts.asStateFlow()

    private val _recentClasses = MutableStateFlow<List<UserProfile.Student>>(emptyList())
    val recentClasses: StateFlow<List<UserProfile.Student>> = _recentClasses.asStateFlow()

    private val _themeOverride = MutableStateFlow("system")
    val themeOverride: StateFlow<String> = _themeOverride.asStateFlow()

    init {
        loadSavedProfile()
        loadNotificationSettings()
        loadAlerts()
        loadRecentClasses()
        _themeOverride.value = prefs.getString("theme_override", "system") ?: "system"
        _lastCheckedTime.value = prefs.getString("last_checked_time", "Offline copy") ?: "Offline copy"
        _isLiveSource.value = prefs.getBoolean("is_live_source", false)
        NotificationHelper.createNotificationChannel(context)
    }

    fun loadAlerts() {
        val json = prefs.getString("recent_alerts_json", null)
        if (!json.isNullOrBlank()) {
            try {
                _alerts.value = alertsListAdapter.fromJson(json) ?: emptyList()
            } catch (_: Exception) {}
        }
    }

    fun saveAlerts(list: List<AlertItem>) {
        val updated = list.take(30)
        _alerts.value = updated
        try {
            val json = alertsListAdapter.toJson(updated)
            prefs.edit().putString("recent_alerts_json", json).apply()
        } catch (_: Exception) {}
    }

    fun addAlert(title: String, body: String) {
        val newItem = AlertItem(
            id = java.util.UUID.randomUUID().toString(),
            title = title,
            body = body,
            timestamp = System.currentTimeMillis(),
            read = false
        )
        val current = _alerts.value.toMutableList()
        current.add(0, newItem)
        saveAlerts(current)
    }

    fun markAllAlertsAsRead() {
        val current = _alerts.value.map { it.copy(read = true) }
        saveAlerts(current)
    }

    fun loadRecentClasses() {
        val json = prefs.getString("recent_classes_json", null)
        if (!json.isNullOrBlank()) {
            try {
                _recentClasses.value = studentListAdapter.fromJson(json) ?: emptyList()
            } catch (_: Exception) {}
        }
    }

    fun saveRecentClasses(list: List<UserProfile.Student>) {
        val updated = list.distinctBy { it.programCode + "-" + it.semester + "-" + (it.section ?: "") + "-" + it.shift }.take(3)
        _recentClasses.value = updated
        try {
            val json = studentListAdapter.toJson(updated)
            prefs.edit().putString("recent_classes_json", json).apply()
        } catch (_: Exception) {}
    }

    fun addRecentClass(student: UserProfile.Student) {
        val current = _recentClasses.value.toMutableList()
        current.removeAll { it.programCode == student.programCode && it.semester == student.semester && it.shift == student.shift && it.section == student.section }
        current.add(0, student)
        saveRecentClasses(current)
    }

    fun saveThemeOverride(theme: String) {
        _themeOverride.value = theme
        prefs.edit().putString("theme_override", theme).apply()
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    fun loadNotificationSettings(): NotificationSettings {
        val settings = NotificationSettings(
            enabled = prefs.getBoolean("notif_enabled", true),
            leadMinutes = prefs.getInt("notif_lead_minutes", 10),
            timetableUpdatesAlert = prefs.getBoolean("notif_updates_alert", true),
            morningSummaryEnabled = prefs.getBoolean("notif_morning_summary_enabled", false),
            morningSummaryTime = prefs.getString("notif_morning_summary_time", "07:00") ?: "07:00",
            lastPermissionPromptTime = prefs.getLong("notif_last_prompt_time", 0L),
            hasAskedPermission = prefs.getBoolean("notif_has_asked_permission", false)
        )
        _notificationSettings.value = settings
        return settings
    }

    fun saveNotificationSettings(settings: NotificationSettings) {
        _notificationSettings.value = settings
        prefs.edit()
            .putBoolean("notif_enabled", settings.enabled)
            .putInt("notif_lead_minutes", settings.leadMinutes)
            .putBoolean("notif_updates_alert", settings.timetableUpdatesAlert)
            .putBoolean("notif_morning_summary_enabled", settings.morningSummaryEnabled)
            .putString("notif_morning_summary_time", settings.morningSummaryTime)
            .putLong("notif_last_prompt_time", settings.lastPermissionPromptTime)
            .putBoolean("notif_has_asked_permission", settings.hasAskedPermission)
            .apply()

        rescheduleNotifications()
    }

    fun rescheduleNotifications() {
        val settings = _notificationSettings.value
        val data = _timetableState.value
        val profile = _userProfile.value
        val classId = when (profile) {
            is UserProfile.Student -> profile.toFilter().classId
            is UserProfile.Teacher -> "teacher:${profile.teacherKey}"
            null -> null
        }

        val now = Calendar.getInstance(TimeZone.getTimeZone("Asia/Karachi"))
        val list = NotificationScheduler.buildSchedule(
            data = data,
            classId = classId,
            settings = settings,
            nowCalendar = now,
            horizonDays = 7,
            teacherIndex = _teacherIndex.value
        )

        _scheduledNotifications.value = list
        NotificationHelper.scheduleAll(context, list)
    }

    suspend fun initialize() = withContext(Dispatchers.IO) {
        // 1. Try to load cached timetable from SharedPreferences
        val cachedJson = prefs.getString("cached_timetable_json", null)
        var data: TimetableData? = null
        if (!cachedJson.isNullOrBlank()) {
            try {
                data = dataAdapter.fromJson(cachedJson)
            } catch (_: Exception) {}
        }

        // 2. If no cache, parse bundled CSV or fallback to seed
        if (data == null || data.classes.isEmpty()) {
            data = loadBundledTimetable()
        }

        updateTimetableData(data)
        rescheduleNotifications()

        // 3. Attempt background fetch if custom URL or default URL is configured
        checkAndUpdateTimetable(isUserTriggered = false)
    }

    private fun updateTimetableData(data: TimetableData) {
        _timetableState.value = data
        val index = TeacherNormalizer.buildTeacherIndex(data, data.meta.department)
        _teacherIndex.value = index

        // Validate current profile against new timetable data
        val currentProfile = _userProfile.value
        if (currentProfile is UserProfile.Student) {
            val classFilter = currentProfile.toFilter()
            val exists = data.classes.any { it.id == classFilter.classId }
            if (exists) {
                _selectedClass.value = classFilter
            } else {
                _syncMessage.value = "Your saved class is not in the updated timetable. Please select your class again."
            }
        } else if (currentProfile is UserProfile.Teacher) {
            val exists = index.teachers.any { it.key == currentProfile.teacherKey }
            if (!exists) {
                _syncMessage.value = "Your saved teacher profile is not in the updated timetable. Please select again."
            }
        }
    }

    suspend fun checkAndUpdateTimetable(isUserTriggered: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        val rawSourceUrl = getCustomSourceUrl().ifEmpty { AppConfig.DEFAULT_TIMETABLE_SOURCE_URL }
        val finalUrl = AppConfig.normalizeSourceUrl(rawSourceUrl)

        val nowFormatted = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date())

        if (finalUrl.isBlank()) {
            _lastCheckedTime.value = "Offline copy"
            prefs.edit().putString("last_checked_time", _lastCheckedTime.value).apply()
            return@withContext false
        }

        _isRefreshing.value = true
        try {
            val request = Request.Builder()
                .url(finalUrl)
                .header("User-Agent", "Mozilla/5.0 (Android; TU-Timetable)")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                _syncMessage.value = "Server returned code: ${response.code}"
                return@withContext false
            }

            val bodyText = response.body?.string() ?: ""
            if (bodyText.isBlank() || bodyText.contains("<html", ignoreCase = true)) {
                _syncMessage.value = "Using saved timetable (could not read remote sheet)"
                return@withContext false
            }

            val newHash = hashString(bodyText)
            val oldHash = prefs.getString("last_content_hash", "")

            val parsedData = TimetableParser.parseCsv(bodyText, "Remote University Sheet")
            if (parsedData.classes.isNotEmpty() && parsedData.timetable.isNotEmpty()) {
                val json = dataAdapter.toJson(parsedData)
                prefs.edit()
                    .putString("cached_timetable_json", json)
                    .putString("last_content_hash", newHash)
                    .putString("last_checked_time", nowFormatted)
                    .putBoolean("is_live_source", true)
                    .apply()

                _isLiveSource.value = true
                _lastCheckedTime.value = nowFormatted
                updateTimetableData(parsedData)

                if (newHash != oldHash) {
                    _syncMessage.value = "Timetable updated to ${parsedData.meta.version}"
                    if (_notificationSettings.value.timetableUpdatesAlert) {
                        NotificationHelper.sendImmediateNotification(
                            context = context,
                            title = "Timetable Updated",
                            body = "Timetable updated to ${parsedData.meta.version}. Tap to check your classes.",
                            notificationId = 888888
                        )
                    }
                } else if (isUserTriggered) {
                    _syncMessage.value = "Timetable is already up to date"
                }

                rescheduleNotifications()
                return@withContext true
            } else {
                _syncMessage.value = "Using saved timetable (invalid response format)"
                return@withContext false
            }
        } catch (e: Exception) {
            _syncMessage.value = "Using saved timetable (last updated ${_lastCheckedTime.value})"
            return@withContext false
        } finally {
            _isRefreshing.value = false
        }
    }

    private fun loadBundledTimetable(): TimetableData {
        return try {
            val assetManager = context.assets
            val inputStream = assetManager.open("TT_DCSIT_FALL_2026_V_1_6.csv")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val csvContent = reader.use { it.readText() }
            TimetableParser.parseCsv(csvContent, "Bundled Fall 2026 V1.6")
        } catch (e: Exception) {
            TimetableData(
                meta = com.example.model.MetaData(),
                timeSlots = emptyList(),
                days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday"),
                classes = emptyList(),
                timetable = emptyMap()
            )
        }
    }

    fun getCustomSourceUrl(): String {
        return prefs.getString("custom_source_url", "") ?: ""
    }

    fun saveCustomSourceUrl(url: String) {
        prefs.edit().putString("custom_source_url", url.trim()).apply()
    }

    fun saveUserProfile(profile: UserProfile) {
        _userProfile.value = profile
        val editor = prefs.edit()

        when (profile) {
            is UserProfile.Student -> {
                editor.putString("profile_role", "student")
                    .putString("student_department", profile.department)
                    .putString("student_program_code", profile.programCode)
                    .putString("student_program_name", profile.programName)
                    .putInt("student_semester", profile.semester)
                    .putString("student_shift", profile.shift)
                    .putString("student_section", profile.section)
                    .remove("teacher_name")
                    .remove("teacher_key")
                _selectedClass.value = profile.toFilter()
                addRecentClass(profile)
            }
            is UserProfile.Teacher -> {
                editor.putString("profile_role", "teacher")
                    .putString("teacher_department", profile.department)
                    .putString("teacher_name", profile.teacherName)
                    .putString("teacher_key", profile.teacherKey)
                    .remove("student_department")
                    .remove("student_program_code")
                    .remove("student_program_name")
                    .remove("student_semester")
                    .remove("student_shift")
                    .remove("student_section")
            }
        }
        editor.apply()
        rescheduleNotifications()
    }

    fun clearUserProfile() {
        _userProfile.value = null
        _selectedClass.value = null
        prefs.edit()
            .remove("profile_role")
            .remove("student_department")
            .remove("student_program_code")
            .remove("student_program_name")
            .remove("student_semester")
            .remove("student_shift")
            .remove("student_section")
            .remove("teacher_department")
            .remove("teacher_name")
            .remove("teacher_key")
            .apply()
        rescheduleNotifications()
    }

    private fun loadSavedProfile() {
        val role = prefs.getString("profile_role", null) ?: return
        if (role == "student") {
            val dept = prefs.getString("student_department", null)
            val pCode = prefs.getString("student_program_code", null)
            val pName = prefs.getString("student_program_name", null)
            val sem = prefs.getInt("student_semester", -1)
            val shift = prefs.getString("student_shift", null)
            val section = prefs.getString("student_section", null)

            if (dept != null && pCode != null && pName != null && sem != -1 && shift != null) {
                val profile = UserProfile.Student(
                    department = dept,
                    programCode = pCode,
                    programName = pName,
                    semester = sem,
                    shift = shift,
                    section = section
                )
                _userProfile.value = profile
                _selectedClass.value = profile.toFilter()
            }
        } else if (role == "teacher") {
            val dept = prefs.getString("teacher_department", "Department of Computer Science & IT") ?: "Department of Computer Science & IT"
            val name = prefs.getString("teacher_name", null)
            val key = prefs.getString("teacher_key", null)
            if (name != null && key != null) {
                _userProfile.value = UserProfile.Teacher(
                    department = dept,
                    teacherName = name,
                    teacherKey = key
                )
            }
        }
    }

    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun getCustomUserName(): String? = prefs.getString("custom_user_name", null)

    fun saveCustomUserName(name: String) {
        prefs.edit().putString("custom_user_name", name).apply()
    }

    fun getCustomProfileImage(): String? = prefs.getString("custom_profile_image", null)

    fun saveCustomProfileImage(uriStr: String) {
        prefs.edit().putString("custom_profile_image", uriStr).apply()
    }

    fun registerLocalAccount(email: String, password: String, displayName: String): Boolean {
        val normalizedEmail = email.trim().lowercase()
        val key = "user_pwd_" + hashString(normalizedEmail)
        val nameKey = "user_name_" + hashString(normalizedEmail)
        prefs.edit()
            .putString(key, hashString(password))
            .putString(nameKey, displayName)
            .apply()
        return true
    }

    fun hasLocalAccount(email: String): Boolean {
        val normalizedEmail = email.trim().lowercase()
        val key = "user_pwd_" + hashString(normalizedEmail)
        return prefs.contains(key)
    }

    fun verifyLocalAccount(email: String, password: String): Boolean {
        val normalizedEmail = email.trim().lowercase()
        val key = "user_pwd_" + hashString(normalizedEmail)
        val storedHash = prefs.getString(key, null) ?: return false
        return storedHash == hashString(password)
    }

    fun getLocalAccountName(email: String): String? {
        val normalizedEmail = email.trim().lowercase()
        val nameKey = "user_name_" + hashString(normalizedEmail)
        return prefs.getString(nameKey, null)
    }

    fun saveAuthSession(user: AuthUser?) {
        val editor = prefs.edit()
        if (user == null) {
            editor.remove("auth_session_uid")
                .remove("auth_session_email")
                .remove("auth_session_name")
                .remove("auth_session_photo")
                .remove("auth_session_is_google")
        } else {
            editor.putString("auth_session_uid", user.uid)
                .putString("auth_session_email", user.email)
                .putString("auth_session_name", user.displayName)
                .putString("auth_session_photo", user.photoUrl)
                .putBoolean("auth_session_is_google", user.isGoogleAccount)
        }
        editor.apply()
    }

    fun getSavedAuthSession(): AuthUser? {
        val uid = prefs.getString("auth_session_uid", null) ?: return null
        val email = prefs.getString("auth_session_email", null)
        val name = prefs.getString("auth_session_name", null)
        val photo = prefs.getString("auth_session_photo", null)
        val isGoogle = prefs.getBoolean("auth_session_is_google", false)
        return AuthUser(
            uid = uid,
            email = email,
            displayName = name,
            photoUrl = photo,
            isGoogleAccount = isGoogle
        )
    }
}
