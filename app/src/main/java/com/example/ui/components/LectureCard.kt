package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Lecture
import com.example.ui.theme.AppTheme

@Composable
fun LectureCard(
    lecture: Lecture,
    isHappeningNow: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors

    val cardBorder = if (isHappeningNow) {
        BorderStroke(2.dp, colors.success)
    } else {
        BorderStroke(1.dp, colors.line)
    }

    val cardColor = if (isHappeningNow) {
        colors.successSoft
    } else {
        colors.surface
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("lecture_card_${lecture.code ?: lecture.subject.replace(" ", "_")}"),
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
            // Row 1: Time range & Live Now / Type Badges
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
                    if (isHappeningNow) {
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = colors.success,
                            modifier = Modifier.testTag("happening_now_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(colors.surface)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "NOW",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = colors.surface
                                )
                            }
                        }
                    }

                    // Theory or Lab badge
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
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = badgeText
                            )
                        }
                    }

                    // Course code chip
                    if (!lecture.code.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = colors.surface2,
                            border = BorderStroke(1.dp, colors.line)
                        ) {
                            Text(
                                text = lecture.code,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
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

            // Row 3: Teacher & Prominent Room Display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Teacher Info
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
                            imageVector = Icons.Default.Person,
                            contentDescription = "Teacher",
                            modifier = Modifier.size(18.dp),
                            tint = colors.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "TEACHER",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.Bold,
                            color = colors.muted
                        )
                        Text(
                            text = lecture.teacher ?: "Teacher not announced",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.text
                        )
                    }
                }

                // Prominent Room Box: --primary-soft bg, --primary text, --line border
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
