package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Lecture
import com.example.model.UserProfile
import com.example.model.toFilter
import com.example.ui.components.*
import com.example.ui.theme.AppTheme
import com.example.util.FormatUtils.getOrdinal
import com.example.util.FormatUtils.parseMinutes
import com.example.viewmodel.TimetableViewModel
import com.example.viewmodel.TimetableViewMode
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import java.util.Calendar
import java.util.TimeZone

enum class ScreenTab {
    HOME,
    WEEK,
    TEACHERS,
    MENU
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    viewModel: TimetableViewModel
) {
    val colors = AppTheme.colors
    val timetableData by viewModel.timetableData.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val selectedDay by viewModel.selectedDay.collectAsState()
    val showWizard by viewModel.showWizard.collectAsState()
    val showAdminDialog by viewModel.showAdminDialog.collectAsState()
    val showDebugScreen by viewModel.showDebugScreen.collectAsState()
    val showNotificationSettingsScreen by viewModel.showNotificationSettingsScreen.collectAsState()
    val notifSettings by viewModel.notificationSettings.collectAsState()
    val lastCheckedTime by viewModel.lastCheckedTime.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isLiveSource by viewModel.isLiveSource.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    val alertsList by viewModel.alerts.collectAsState()
    val showProfileScreen by viewModel.showProfileScreen.collectAsState()
    val customProfileImage by viewModel.customProfileImage.collectAsState()

    var currentTab by remember { mutableStateOf(ScreenTab.HOME) }
    var showSwitchClassSheet by remember { mutableStateOf(false) }
    var showAlertsScreen by remember { mutableStateOf(false) }
    var showNotifPrompt by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Check if permission prompt needs to be shown
    LaunchedEffect(userProfile, notifSettings) {
        if (userProfile != null && !notifSettings.hasAskedPermission) {
            val threeDaysMillis = 3 * 24 * 60 * 60 * 1000L
            if (System.currentTimeMillis() - notifSettings.lastPermissionPromptTime >= threeDaysMillis) {
                showNotifPrompt = true
            }
        }
    }

    // Handle toast/snackbar sync messages
    LaunchedEffect(syncMessage) {
        syncMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSyncMessage()
        }
    }

    if (showWizard || userProfile == null) {
        WelcomeWizardScreen(viewModel = viewModel)
        return
    }

    if (showDebugScreen) {
        BackHandler { viewModel.showDebug(false) }
        DebugScreen(
            viewModel = viewModel,
            onBack = { viewModel.showDebug(false) }
        )
        return
    }

    if (showNotificationSettingsScreen) {
        NotificationSettingsScreen(
            viewModel = viewModel,
            onBack = { viewModel.showNotificationSettings(false) }
        )
        return
    }

    if (showAlertsScreen) {
        AlertsScreen(
            viewModel = viewModel,
            onBack = { showAlertsScreen = false }
        )
        return
    }

    if (showProfileScreen) {
        ProfileScreen(
            viewModel = viewModel,
            onBack = { viewModel.showProfile(false) }
        )
        return
    }

    val currentProfile = userProfile
    if (currentProfile is UserProfile.Teacher) {
        TeacherTimetableScreen(
            viewModel = viewModel,
            profile = currentProfile
        )
        return
    }

    val daysList = timetableData?.days ?: listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")

    val studentProfile = currentProfile as? UserProfile.Student
    val currentClassId = studentProfile?.toFilter()?.classId

    val classTimetable = remember(currentClassId, timetableData) {
        val map = currentClassId?.let { timetableData?.timetable?.get(it) }
        map ?: emptyMap<String, List<Lecture>>()
    }

    val lecturesForDay = classTimetable[selectedDay] ?: emptyList()

    val dayScheduleItems = remember(lecturesForDay) {
        buildScheduleItems(lecturesForDay)
    }

    // Determine Avatar initials
    val initials = remember(studentProfile) {
        val code = studentProfile?.programCode ?: ""
        when {
            code.contains("CS", ignoreCase = true) -> "CS"
            code.contains("IT", ignoreCase = true) -> "IT"
            code.contains("SE", ignoreCase = true) -> "SE"
            code.contains("AI", ignoreCase = true) -> "AI"
            else -> "TU"
        }
    }

    // Unread Alerts Check
    val unreadAlertsCount = remember(alertsList) {
        alertsList.count { !it.read }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = colors.bg,
        topBar = {
            if (currentTab != ScreenTab.MENU) {
                Surface(
                    color = colors.bg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: Round Avatar and Greetings block
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.showProfile(true) }
                                .testTag("header_greeting_block")
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = colors.primarySoft,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (customProfileImage.isNotBlank()) {
                                        AsyncImage(
                                            model = customProfileImage,
                                            contentDescription = "Profile Photo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Text(
                                            text = initials,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.accentText
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = getKarachiGreeting(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.muted
                                )
                                val className = studentProfile?.let {
                                    val shiftName = if (it.shift == "regular") "First Time" else "Second Time"
                                    "${it.programCode} ${it.semester}${getOrdinal(it.semester)} Sem, $shiftName"
                                } ?: "No Class Selected"
                                Text(
                                    text = className,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.text,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Right: Calendar & Bell Buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Calendar button
                            Surface(
                                shape = CircleShape,
                                color = colors.surface,
                                border = BorderStroke(1.dp, colors.line),
                                modifier = Modifier
                                    .size(40.dp)
                                    .clickable { currentTab = ScreenTab.WEEK }
                                    .testTag("header_calendar_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = "Week Schedule",
                                        tint = colors.text,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Bell button with dot
                            Surface(
                                shape = CircleShape,
                                color = colors.surface,
                                border = BorderStroke(1.dp, colors.line),
                                modifier = Modifier
                                    .size(40.dp)
                                    .clickable { showAlertsScreen = true }
                                    .testTag("header_alerts_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Alerts",
                                        tint = colors.text,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    if (unreadAlertsCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(6.dp)
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(colors.danger)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            // FIXED BOTTOM NAVIGATION BAR WITH RAISED CENTER BUTTON (Overlaid in a Box to prevent parent clipping)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.BottomCenter
            ) {
                // The main bottom bar Surface background
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp),
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.line)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Item 1: Home
                        BottomNavItem(
                            label = "Home",
                            icon = Icons.Default.Home,
                            isSelected = currentTab == ScreenTab.HOME,
                            onClick = { currentTab = ScreenTab.HOME },
                            modifier = Modifier.weight(1f)
                        )

                        // Item 2: Week
                        BottomNavItem(
                            label = "Week",
                            icon = Icons.Default.CalendarToday,
                            isSelected = currentTab == ScreenTab.WEEK,
                            onClick = { currentTab = ScreenTab.WEEK },
                            modifier = Modifier.weight(1f)
                        )

                        // reserved space spacer
                        Spacer(modifier = Modifier.weight(1f))

                        // Item 4: Teachers
                        BottomNavItem(
                            label = "Teachers",
                            icon = Icons.Default.People,
                            isSelected = currentTab == ScreenTab.TEACHERS,
                            onClick = { currentTab = ScreenTab.TEACHERS },
                            modifier = Modifier.weight(1f)
                        )

                        // Item 5: Menu
                        BottomNavItem(
                            label = "Menu",
                            icon = Icons.Default.GridOn,
                            isSelected = currentTab == ScreenTab.MENU,
                            onClick = { currentTab = ScreenTab.MENU },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Raised center action button overlay
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                ) {
                    Surface(
                        onClick = { showSwitchClassSheet = true },
                        shape = CircleShape,
                        color = colors.primary,
                        border = BorderStroke(4.dp, colors.surface),
                        modifier = Modifier
                            .size(56.dp)
                            .testTag("center_switch_button"),
                        shadowElevation = 6.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Switch Class",
                                tint = colors.accentText,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Switch",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = colors.muted
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentTab) {
                ScreenTab.HOME -> {
                    // Today and next lecture calculations
                    val karachiTz = TimeZone.getTimeZone("Asia/Karachi")
                    val nowCal = Calendar.getInstance(karachiTz)
                    val todayDayName = TimetableViewModel.getKarachiDefaultDay()

                    val lecturesToday = classTimetable[todayDayName] ?: emptyList()

                    val runningLec = remember(lecturesToday) {
                        lecturesToday.find { isLecNow(it, nowCal) }
                    }

                    val nextLec = remember(lecturesToday) {
                        val sorted = lecturesToday.sortedBy { parseMinutes(it.start) }
                        val nowMinutes = nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE)
                        sorted.find { parseMinutes(it.start) > nowMinutes }
                    }

                    val minutesLeft = remember(nextLec) {
                        if (nextLec != null) {
                            val nowMinutes = nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE)
                            parseMinutes(nextLec.start) - nowMinutes
                        } else {
                            0
                        }
                    }

                    val completedPercent = remember(runningLec) {
                        if (runningLec != null) {
                            val nowMinutes = nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE)
                            val start = parseMinutes(runningLec.start)
                            val end = parseMinutes(runningLec.end)
                            val total = end - start
                            if (total > 0) {
                                ((nowMinutes - start).toFloat() / total.toFloat()).coerceIn(0f, 1f)
                            } else 0f
                        } else {
                            0f
                        }
                    }

                    var dragAmount by remember { mutableStateOf(0f) }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp)
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDrag = { change, dragAmountOffset ->
                                        change.consume()
                                        dragAmount += dragAmountOffset.x
                                    },
                                    onDragEnd = {
                                        val currentIndex = daysList.indexOf(selectedDay)
                                        if (dragAmount < -150f && currentIndex < daysList.size - 1) {
                                            viewModel.setSelectedDay(daysList[currentIndex + 1])
                                        } else if (dragAmount > 150f && currentIndex > 0) {
                                            viewModel.setSelectedDay(daysList[currentIndex - 1])
                                        }
                                        dragAmount = 0f
                                    }
                                )
                            }
                    ) {
                        item { Spacer(modifier = Modifier.height(2.dp)) }

                        // 1. Calendar Day Strip Card
                        item {
                            DayStripCard(
                                daysList = daysList,
                                selectedDay = selectedDay,
                                onDaySelected = { viewModel.setSelectedDay(it) },
                                timetableMap = classTimetable
                            )
                        }

                        // 2. Next Lecture Hero Card (follows today, ignores selected day)
                        item {
                            HeroLectureCard(
                                currentLecture = runningLec,
                                nextLecture = nextLec,
                                nextMinutesLeft = minutesLeft,
                                completedPercent = completedPercent
                            )
                        }

                        // Showing custom day helper text if selectedDay is not today
                        if (!selectedDay.equals(todayDayName, ignoreCase = true)) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(100.dp),
                                    color = colors.surface2,
                                    border = BorderStroke(1.dp, colors.line)
                                ) {
                                    Text(
                                        text = "Showing schedule for $selectedDay",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.primary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        // 3. Two side-by-side stat cards
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val lecturesTodayCount = lecturesToday.size
                                val firstLectureStart = lecturesToday.minByOrNull { parseMinutes(it.start) }?.start ?: "No classes"

                                // Stat Card 1: Today's Classes
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = colors.surface,
                                    border = BorderStroke(1.dp, colors.line),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Icon(Icons.Default.Class, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Lectures Today", style = MaterialTheme.typography.labelSmall, color = colors.muted)
                                        Text(
                                            text = if (lecturesTodayCount > 0) "$lecturesTodayCount lectures" else "Free day",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.text
                                        )
                                    }
                                }

                                // Stat Card 2: First Lecture time
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = colors.surface,
                                    border = BorderStroke(1.dp, colors.line),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("First Lecture", style = MaterialTheme.typography.labelSmall, color = colors.muted)
                                        Text(
                                            text = firstLectureStart,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.text
                                        )
                                    }
                                }
                            }
                        }

                        // 4. Lecture List for selected day
                        if (dayScheduleItems.isEmpty()) {
                            item {
                                EmptyDayCard(dayName = selectedDay)
                            }
                        } else {
                            items(dayScheduleItems) { item ->
                                when (item) {
                                    is ScheduleDisplayItem.LectureItem -> {
                                        val isNow = viewModel.isLectureNow(item.lecture, selectedDay)
                                        LectureCard(
                                            lecture = item.lecture,
                                            isHappeningNow = isNow
                                        )
                                    }
                                    is ScheduleDisplayItem.GapItem -> {
                                        FreeGapCard(start = item.start, end = item.end)
                                    }
                                    is ScheduleDisplayItem.PrayerBreakItem -> {
                                        PrayerBreakCard()
                                    }
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(100.dp)) }
                    }
                }
                ScreenTab.WEEK -> {
                    WeekScreen(viewModel = viewModel)
                }
                ScreenTab.TEACHERS -> {
                    TeachersScreen(viewModel = viewModel)
                }
                ScreenTab.MENU -> {
                    MenuScreen(viewModel = viewModel)
                }
            }
        }
    }

    if (showSwitchClassSheet) {
        SwitchClassSheet(
            viewModel = viewModel,
            onDismiss = { showSwitchClassSheet = false }
        )
    }

    if (showAdminDialog) {
        AdminSourceDialog(
            initialUrl = viewModel.getCustomSourceUrl(),
            onDismiss = { viewModel.showAdminSettings(false) },
            onSave = { newUrl -> viewModel.saveCustomSourceUrl(newUrl) }
        )
    }

    if (showNotifPrompt) {
        NotificationPermissionBottomSheet(
            onTurnOn = {
                showNotifPrompt = false
                viewModel.onNotificationPermissionGranted()
            },
            onNotNow = {
                showNotifPrompt = false
                viewModel.onNotificationPermissionPromptDismissed()
            }
        )
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxHeight()
            .clickable { onClick() }
            .testTag("bottom_nav_$label")
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) colors.primary else colors.muted,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) colors.accentText else colors.muted
        )
    }
}

private fun getKarachiGreeting(): String {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Karachi"))
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    return when {
        hour in 5..11 -> "Good morning!"
        hour in 12..16 -> "Good afternoon!"
        hour in 17..20 -> "Good evening!"
        else -> "Hello!"
    }
}

private fun isLecNow(lec: Lecture, cal: Calendar): Boolean {
    val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    return nowMinutes >= parseMinutes(lec.start) && nowMinutes <= parseMinutes(lec.end)
}

@Composable
fun EmptyDayCard(dayName: String) {
    val colors = AppTheme.colors
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = colors.surface2,
        border = BorderStroke(1.dp, colors.line)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Weekend,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = colors.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No Classes on $dayName",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.text
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Enjoy your day off or use the time for independent study!",
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted
            )
        }
    }
}

sealed class ScheduleDisplayItem {
    data class LectureItem(val lecture: Lecture) : ScheduleDisplayItem()
    data class GapItem(val start: String, val end: String) : ScheduleDisplayItem()
    object PrayerBreakItem : ScheduleDisplayItem()
}

fun buildScheduleItems(lectures: List<Lecture>): List<ScheduleDisplayItem> {
    if (lectures.isEmpty()) return emptyList()

    val sorted = lectures.sortedBy { parseMinutes(it.start) }
    val result = mutableListOf<ScheduleDisplayItem>()

    for (i in sorted.indices) {
        val current = sorted[i]
        result.add(ScheduleDisplayItem.LectureItem(current))

        if (i < sorted.size - 1) {
            val next = sorted[i + 1]
            val currentEndMinutes = parseMinutes(current.end)
            val nextStartMinutes = parseMinutes(next.start)

            val prayerStartMinutes = 12 * 60 + 50 // 12:50
            val prayerEndMinutes = 13 * 60 + 10   // 13:10

            // Check if prayer break falls in between
            if (currentEndMinutes <= prayerStartMinutes && nextStartMinutes >= prayerEndMinutes) {
                if (currentEndMinutes < prayerStartMinutes) {
                    result.add(ScheduleDisplayItem.GapItem(current.end, "12:50"))
                }
                result.add(ScheduleDisplayItem.PrayerBreakItem)
                if (nextStartMinutes > prayerEndMinutes) {
                    result.add(ScheduleDisplayItem.GapItem("13:10", next.start))
                }
            } else if (nextStartMinutes > currentEndMinutes) {
                result.add(ScheduleDisplayItem.GapItem(current.end, next.start))
            }
        }
    }

    return result
}
