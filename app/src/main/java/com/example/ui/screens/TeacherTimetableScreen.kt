package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.config.AppConfig
import com.example.model.TeacherLecture
import com.example.model.UserProfile
import com.example.ui.components.AdminSourceDialog
import com.example.ui.components.FreeGapCard
import com.example.ui.components.PrayerBreakCard
import com.example.ui.theme.AppTheme
import com.example.util.FormatUtils.getTomorrowDayName
import com.example.util.FormatUtils.parseMinutes
import com.example.util.FormatUtils.getOrdinal
import com.example.viewmodel.TimetableViewModel
import com.example.viewmodel.TimetableViewMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherTimetableScreen(
    viewModel: TimetableViewModel,
    profile: UserProfile.Teacher
) {
    val colors = AppTheme.colors
    val context = LocalContext.current
    val timetableData by viewModel.timetableData.collectAsState()
    val teacherIndex by viewModel.teacherIndex.collectAsState()
    val selectedDay by viewModel.selectedDay.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val showAdminDialog by viewModel.showAdminDialog.collectAsState()
    val lastCheckedTime by viewModel.lastCheckedTime.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isLiveSource by viewModel.isLiveSource.collectAsState()

    val teacherSchedule = remember(teacherIndex, profile.teacherKey) {
        teacherIndex?.byTeacher?.get(profile.teacherKey) ?: emptyMap()
    }

    val daysList = timetableData?.days ?: listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")

    // Calculate total lectures & total distinct classes taught this week
    val (totalWeekLectures, totalClassesTaught) = remember(teacherSchedule) {
        var lecs = 0
        val classesSet = mutableSetOf<String>()
        for ((_, dayList) in teacherSchedule) {
            lecs += dayList.size
            for (item in dayList) {
                classesSet.addAll(item.classLabels)
            }
        }
        Pair(lecs, classesSet.size)
    }

    Scaffold(
        containerColor = colors.bg,
        contentColor = colors.text,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Class Flow",
                            style = MaterialTheme.typography.titleLarge,
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
                actions = {
                    IconButton(
                        onClick = { viewModel.showNotificationSettings(true) },
                        modifier = Modifier.testTag("teacher_notif_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = colors.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.showProfile(true) },
                        modifier = Modifier.testTag("teacher_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Profile & Settings",
                            tint = colors.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.bg,
                    titleContentColor = colors.text
                )
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshTimetable() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            // Teacher Profile Header Card
            item {
                Spacer(modifier = Modifier.height(2.dp))
                TeacherHeaderBanner(
                    profile = profile,
                    totalWeekLectures = totalWeekLectures,
                    totalClassesTaught = totalClassesTaught,
                    onBannerClick = { viewModel.showProfile(true) }
                )
            }

            // Quick Switches & Day Tabs (Identical to Student side)
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val todayDay = TimetableViewModel.getKarachiDefaultDay()
                            val isToday = selectedDay == todayDay && viewMode == TimetableViewMode.DAY_VIEW
                            FilterChip(
                                selected = isToday,
                                onClick = {
                                    viewModel.setViewMode(TimetableViewMode.DAY_VIEW)
                                    viewModel.setSelectedDay(todayDay)
                                },
                                label = {
                                    Text(
                                        text = "Today",
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                modifier = Modifier.testTag("quick_today_chip"),
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.primary,
                                    selectedLabelColor = colors.onPrimary,
                                    containerColor = colors.surface2,
                                    labelColor = colors.text
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isToday,
                                    borderColor = if (isToday) colors.primary else colors.line
                                )
                            )

                            val tomorrowDay = getTomorrowDayName()
                            val isTomorrow = selectedDay == tomorrowDay && viewMode == TimetableViewMode.DAY_VIEW
                            FilterChip(
                                selected = isTomorrow,
                                onClick = {
                                    viewModel.setViewMode(TimetableViewMode.DAY_VIEW)
                                    viewModel.setSelectedDay(tomorrowDay)
                                },
                                label = {
                                    Text(
                                        text = "Tomorrow",
                                        fontWeight = if (isTomorrow) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                modifier = Modifier.testTag("quick_tomorrow_chip"),
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.primary,
                                    selectedLabelColor = colors.onPrimary,
                                    containerColor = colors.surface2,
                                    labelColor = colors.text
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isTomorrow,
                                    borderColor = if (isTomorrow) colors.primary else colors.line
                                )
                            )
                        }

                        // View Mode Toggle (Day vs Full Week)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surface2)
                                .padding(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (viewMode == TimetableViewMode.DAY_VIEW) colors.primary else Color.Transparent)
                                    .clickable { viewModel.setViewMode(TimetableViewMode.DAY_VIEW) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("day_view_tab")
                            ) {
                                Text(
                                    text = "Day",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (viewMode == TimetableViewMode.DAY_VIEW) colors.onPrimary else colors.muted
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (viewMode == TimetableViewMode.FULL_WEEK_VIEW) colors.primary else Color.Transparent)
                                    .clickable { viewModel.setViewMode(TimetableViewMode.FULL_WEEK_VIEW) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("week_view_tab")
                            ) {
                                Text(
                                    text = "Week",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (viewMode == TimetableViewMode.FULL_WEEK_VIEW) colors.onPrimary else colors.muted
                                )
                            }
                        }
                    }

                    // Day Tabs Row (Only shown in Day View)
                    if (viewMode == TimetableViewMode.DAY_VIEW) {
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(daysList) { day ->
                                val isSelected = day.equals(selectedDay, ignoreCase = true)
                                val dayLectures = teacherSchedule[day] ?: emptyList()
                                val lectureCount = dayLectures.size

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) colors.primary else colors.surface,
                                    border = BorderStroke(1.2.dp, if (isSelected) colors.primary else colors.line),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { viewModel.setSelectedDay(day) }
                                        .testTag("day_tab_$day")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = day.take(3),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) colors.onPrimary else colors.text
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (lectureCount > 0) "$lectureCount lecs" else "Off",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) colors.onPrimary.copy(alpha = 0.85f) else colors.muted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Day Schedule or Full Week
            if (viewMode == TimetableViewMode.DAY_VIEW) {
                val dayLectures = teacherSchedule[selectedDay] ?: emptyList()
                val nextLecture = viewModel.getNextTeacherLecture(dayLectures, selectedDay)

                if (nextLecture != null) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.accentSoft,
                            border = BorderStroke(1.2.dp, colors.line),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Upcoming,
                                    contentDescription = null,
                                    tint = colors.accentText,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Next: ${nextLecture.start}, ${if (nextLecture.room == "CL") "Computer Lab (CL)" else nextLecture.room ?: "Room TBA"}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.accentText
                                    )
                                    Text(
                                        text = "${nextLecture.subject} • ${nextLecture.classLabels.joinToString(", ")}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.text,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                if (dayLectures.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = colors.surface2,
                            border = BorderStroke(1.2.dp, colors.line),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Celebration,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = colors.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No teaching schedule for $selectedDay",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.text
                                )
                                Text(
                                    text = "Enjoy your research and prep time!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.muted
                                )
                            }
                        }
                    }
                } else {
                    val renderedItems = buildTeacherScheduleItems(dayLectures)
                    items(renderedItems) { item ->
                        when (item) {
                            is TeacherScheduleItem.LectureCardItem -> {
                                val isNow = viewModel.isTeacherLectureNow(item.lecture, selectedDay)
                                TeacherLectureCard(lecture = item.lecture, isHappeningNow = isNow)
                            }
                            is TeacherScheduleItem.GapItem -> {
                                FreeGapCard(start = item.start, end = item.end)
                            }
                            is TeacherScheduleItem.PrayerBreakItem -> {
                                PrayerBreakCard()
                            }
                        }
                    }
                }
            } else {
                // Full Week View
                daysList.forEach { dayName ->
                    val dayLectures = teacherSchedule[dayName] ?: emptyList()
                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = colors.surface2,
                            border = BorderStroke(1.dp, colors.line),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = colors.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = dayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.text
                                )
                            }
                        }
                    }

                    if (dayLectures.isEmpty()) {
                        item {
                            Text(
                                text = "No teaching lectures",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.muted,
                                modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 8.dp)
                            )
                        }
                    } else {
                        val renderedItems = buildTeacherScheduleItems(dayLectures)
                        items(renderedItems) { item ->
                            when (item) {
                                is TeacherScheduleItem.LectureCardItem -> {
                                    val isNow = viewModel.isTeacherLectureNow(item.lecture, dayName)
                                    TeacherLectureCard(lecture = item.lecture, isHappeningNow = isNow)
                                }
                                is TeacherScheduleItem.GapItem -> {
                                    FreeGapCard(start = item.start, end = item.end)
                                }
                                is TeacherScheduleItem.PrayerBreakItem -> {
                                    PrayerBreakCard()
                                }
                            }
                        }
                    }
                }
            }

            // Footer
            item {
                Spacer(modifier = Modifier.height(16.dp))
                TeacherFooterSection(
                    version = timetableData?.meta?.version ?: "V1.6",
                    lastCheckedTime = lastCheckedTime,
                    isLiveSource = isLiveSource,
                    onVersionTap = { viewModel.onVersionTapped() }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
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
fun TeacherHeaderBanner(
    profile: UserProfile.Teacher,
    totalWeekLectures: Int,
    totalClassesTaught: Int,
    onBannerClick: () -> Unit
) {
    val colors = AppTheme.colors
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onBannerClick() }
            .testTag("teacher_header_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface2),
        border = BorderStroke(1.2.dp, colors.line),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(colors.primarySoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CoPresent,
                            contentDescription = "Teacher",
                            tint = colors.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
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

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View Profile",
                    tint = colors.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = colors.surface,
                border = BorderStroke(1.dp, colors.line),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Analytics,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = colors.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Total this week: $totalWeekLectures lectures, $totalClassesTaught classes",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.text
                    )
                }
            }
        }
    }
}

@Composable
fun TeacherLectureCard(
    lecture: TeacherLecture,
    isHappeningNow: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors

    val cardBorder = if (isHappeningNow) {
        BorderStroke(2.dp, colors.success)
    } else if (lecture.clash) {
        BorderStroke(1.5.dp, colors.danger)
    } else {
        BorderStroke(1.2.dp, colors.line)
    }

    val cardColor = if (isHappeningNow) colors.successSoft else colors.surface

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("teacher_lecture_card_${lecture.code ?: lecture.subject}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Row 1: Time, Clash badge, Now badge, Type & Code
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (isHappeningNow) colors.success else colors.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${lecture.start} - ${lecture.end}",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                        fontWeight = FontWeight.Bold,
                        color = colors.text
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (lecture.clash) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = colors.dangerSoft,
                            border = BorderStroke(1.dp, colors.danger)
                        ) {
                            Text(
                                text = "CLASH",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.danger
                            )
                        }
                    }

                    if (isHappeningNow) {
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = colors.success
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(colors.surface)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "NOW",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = colors.surface
                                )
                            }
                        }
                    }

                    if (!lecture.type.isNullOrBlank()) {
                        val isTheory = lecture.type.equals("Theory", ignoreCase = true)
                        val badgeBg = if (isTheory) colors.primarySoft else colors.infoLabSoft
                        val badgeText = if (isTheory) colors.primary else colors.infoLab

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = badgeBg,
                            border = BorderStroke(1.dp, colors.line)
                        ) {
                            Text(
                                text = lecture.type,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = badgeText
                            )
                        }
                    }

                    if (!lecture.code.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = colors.surface2,
                            border = BorderStroke(1.dp, colors.line)
                        ) {
                            Text(
                                text = lecture.code,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.text
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Subject Title
            Text(
                text = lecture.subject,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                fontWeight = FontWeight.Bold,
                color = colors.text
            )

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = colors.line
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Row 3: Class Labels & Prominent Room Display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Classes Info
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(colors.surface2),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = "Classes",
                            modifier = Modifier.size(18.dp),
                            tint = colors.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "CLASSES",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.Bold,
                            color = colors.muted
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            lecture.classLabels.forEach { label ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = colors.primarySoft,
                                    border = BorderStroke(1.dp, colors.line)
                                ) {
                                    Text(
                                        text = label,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.primary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Prominent Room Box
                val roomText = when (lecture.room) {
                    null -> "Room TBA"
                    "CL" -> "Computer Lab (CL)"
                    else -> lecture.room
                }
                val hasRoom = lecture.room != null

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (hasRoom) colors.primarySoft else colors.surface2,
                    border = BorderStroke(1.2.dp, colors.line)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MeetingRoom,
                            contentDescription = "Room",
                            modifier = Modifier.size(16.dp),
                            tint = colors.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "ROOM",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.primary
                            )
                            Text(
                                text = roomText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.accentText
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeacherFooterSection(
    version: String,
    lastCheckedTime: String,
    isLiveSource: Boolean,
    onVersionTap: () -> Unit
) {
    val context = LocalContext.current
    val colors = AppTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Class Flow Fall 2026 ($version) • ${if (isLiveSource) "Live from university link" else "Offline copy"}",
            style = MaterialTheme.typography.labelSmall,
            color = colors.muted,
            modifier = Modifier
                .clickable { onVersionTap() }
                .padding(4.dp)
                .testTag("version_text")
        )
        if (lastCheckedTime.isNotBlank() && lastCheckedTime != "Offline copy") {
            Text(
                text = "Last checked: $lastCheckedTime",
                style = MaterialTheme.typography.labelSmall,
                color = colors.muted
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(AppConfig.WHATSAPP_CHANNEL_URL))
                context.startActivity(intent)
            },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.success,
                contentColor = colors.surface
            )
        ) {
            Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Official updates (WhatsApp)", fontWeight = FontWeight.Bold)
        }
    }
}

// Helpers
sealed class TeacherScheduleItem {
    data class LectureCardItem(val lecture: TeacherLecture) : TeacherScheduleItem()
    data class GapItem(val start: String, val end: String) : TeacherScheduleItem()
    object PrayerBreakItem : TeacherScheduleItem()
}

fun buildTeacherScheduleItems(lectures: List<TeacherLecture>): List<TeacherScheduleItem> {
    if (lectures.isEmpty()) return emptyList()

    val sorted = lectures.sortedBy { parseMinutes(it.start) }
    val result = mutableListOf<TeacherScheduleItem>()

    for (i in sorted.indices) {
        val current = sorted[i]
        result.add(TeacherScheduleItem.LectureCardItem(current))

        if (i < sorted.size - 1) {
            val next = sorted[i + 1]
            val currentEndMinutes = parseMinutes(current.end)
            val nextStartMinutes = parseMinutes(next.start)

            val prayerStartMinutes = 12 * 60 + 50 // 12:50
            val prayerEndMinutes = 13 * 60 + 10   // 13:10

            if (currentEndMinutes <= prayerStartMinutes && nextStartMinutes >= prayerEndMinutes) {
                if (currentEndMinutes < prayerStartMinutes) {
                    result.add(TeacherScheduleItem.GapItem(current.end, "12:50"))
                }
                result.add(TeacherScheduleItem.PrayerBreakItem)
                if (nextStartMinutes > prayerEndMinutes) {
                    result.add(TeacherScheduleItem.GapItem("13:10", next.start))
                }
            } else if (nextStartMinutes > currentEndMinutes) {
                result.add(TeacherScheduleItem.GapItem(current.end, next.start))
            }
        }
    }

    return result
}
