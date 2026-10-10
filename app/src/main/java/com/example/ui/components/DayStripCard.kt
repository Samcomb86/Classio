package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Lecture
import com.example.ui.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun DayStripCard(
    daysList: List<String>,
    selectedDay: String,
    onDaySelected: (String) -> Unit,
    timetableMap: Map<String, List<Lecture>>,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val karachiTimeZone = remember { TimeZone.getTimeZone("Asia/Karachi") }

    var weekOffset by remember { mutableStateOf(0) }

    // Resolve date number and month year label based on weekOffset
    val calendarInfo = remember(weekOffset, daysList) {
        val cal = Calendar.getInstance(karachiTimeZone)

        // Find the Monday of the current week
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val daysToSubtract = when (dayOfWeek) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
        cal.add(Calendar.DAY_OF_YEAR, -daysToSubtract)

        // Apply week offset
        cal.add(Calendar.DAY_OF_YEAR, weekOffset * 7)

        val formatMonthYear = SimpleDateFormat("MMMM yyyy", Locale.US).apply { timeZone = karachiTimeZone }
        val monthYearLabel = formatMonthYear.format(cal.time)

        // Calculate dates for each day name in daysList
        val dayDates = daysList.map { dayName ->
            val dayCal = (cal.clone() as Calendar)
            val offsetFromMonday = when (dayName.lowercase(Locale.US)) {
                "monday" -> 0
                "tuesday" -> 1
                "wednesday" -> 2
                "thursday" -> 3
                "friday" -> 4
                "saturday" -> 5
                "sunday" -> 6
                else -> 0
            }
            dayCal.add(Calendar.DAY_OF_YEAR, offsetFromMonday)
            val dateNumber = dayCal.get(Calendar.DAY_OF_MONTH)
            val isToday = isCalendarToday(dayCal, karachiTimeZone)
            Pair(dateNumber, isToday)
        }

        Triple(monthYearLabel, dayDates, weekOffset != 0)
    }

    val (monthYearLabel, dayDates, isNotCurrentWeek) = calendarInfo

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.line),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Month Year & Next Prev Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = monthYearLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.text
                    )
                    if (isNotCurrentWeek) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = colors.primarySoft,
                            modifier = Modifier
                                .clickable { weekOffset = 0 }
                                .testTag("jump_to_today_chip")
                        ) {
                            Text(
                                text = "Today",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.accentText,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Prev Button
                    Surface(
                        shape = CircleShape,
                        color = colors.surface2,
                        border = BorderStroke(1.dp, colors.line),
                        modifier = Modifier
                            .size(34.dp)
                            .clickable { weekOffset -= 1 }
                            .testTag("day_strip_prev_week")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Previous Week",
                                tint = colors.text,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Next Button
                    Surface(
                        shape = CircleShape,
                        color = colors.surface2,
                        border = BorderStroke(1.dp, colors.line),
                        modifier = Modifier
                            .size(34.dp)
                            .clickable { weekOffset += 1 }
                            .testTag("day_strip_next_week")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Next Week",
                                tint = colors.text,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Days Columns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                daysList.forEachIndexed { index, dayName ->
                    val isSelected = dayName.equals(selectedDay, ignoreCase = true)
                    val shortName = dayName.take(3)
                    val (dateNumber, isToday) = dayDates.getOrElse(index) { Pair(1, false) }
                    val hasLectures = timetableMap[dayName]?.isNotEmpty() == true

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onDaySelected(dayName) }
                            .testTag("day_column_$dayName")
                    ) {
                        if (isSelected) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = colors.primarySoft,
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                ) {
                                    Text(
                                        text = shortName,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.muted
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = String.format(Locale.US, "%02d", dateNumber),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.accentText
                                    )
                                }
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 12.dp)
                            ) {
                                Text(
                                    text = shortName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.muted
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = String.format(Locale.US, "%02d", dateNumber),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isToday) colors.primary else colors.text
                                )
                            }
                        }

                        // Lecture Dot Indicator below
                        Box(modifier = Modifier.height(6.dp)) {
                            if (hasLectures) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(colors.primary)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun isCalendarToday(cal: Calendar, tz: TimeZone): Boolean {
    val today = Calendar.getInstance(tz)
    return cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
           cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
}
