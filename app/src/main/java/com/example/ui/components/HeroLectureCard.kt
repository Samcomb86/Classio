package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Lecture
import com.example.ui.theme.AppTheme

@Composable
fun HeroLectureCard(
    currentLecture: Lecture?,
    nextLecture: Lecture?,
    nextMinutesLeft: Int,
    completedPercent: Float,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors

    val isRunning = currentLecture != null
    val targetLecture = currentLecture ?: nextLecture

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.primarySoft,
        modifier = modifier.fillMaxWidth()
    ) {
        if (targetLecture == null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "All done for today!",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                    fontWeight = FontWeight.Bold,
                    color = colors.accentText
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "You are free for the rest of the day. Enjoy your study and rest time!",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1.3f)) {
                    // Title Label (Now / Next)
                    Text(
                        text = if (isRunning) "HAPPENING NOW" else "Next lecture in $nextMinutesLeft min",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.accentText,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // Subject Title
                    Text(
                        text = targetLecture.subject,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, lineHeight = 24.sp),
                        fontWeight = FontWeight.Bold,
                        color = colors.text,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Badges row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!targetLecture.code.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = colors.surface,
                                border = BorderStroke(1.dp, colors.line)
                            ) {
                                Text(
                                    text = targetLecture.code,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.text,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (!targetLecture.type.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = colors.surface2,
                                border = BorderStroke(1.dp, colors.line)
                            ) {
                                Text(
                                    text = targetLecture.type,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.muted,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Teacher
                    Text(
                        text = targetLecture.teacher ?: "Teacher not announced",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = colors.muted
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Time Range & Room Info
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MeetingRoom,
                            contentDescription = null,
                            tint = colors.accentText,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${targetLecture.start} - ${targetLecture.end}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.text
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        val displayRoom = when {
                            targetLecture.room.isNullOrBlank() -> "Room TBA"
                            targetLecture.room.equals("CL", ignoreCase = true) -> "Computer Lab"
                            else -> "Room " + targetLecture.room
                        }
                        Text(
                            text = displayRoom,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.accentText
                        )
                    }
                }

                // Progress ring on right
                Spacer(modifier = Modifier.width(16.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(80.dp)
                        .weight(0.7f, fill = false)
                ) {
                    val trackColor = colors.surface2
                    val accentColor = colors.primary

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = trackColor,
                            style = Stroke(width = 6.dp.toPx())
                        )
                        val angle = if (isRunning) completedPercent * 360f else {
                            val percentLeft = (nextMinutesLeft.toFloat() / 60f).coerceIn(0f, 1f)
                            percentLeft * 360f
                        }
                        drawArc(
                            color = accentColor,
                            startAngle = -90f,
                            sweepAngle = angle,
                            useCenter = false,
                            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (isRunning) {
                            Text(
                                text = String.format(java.util.Locale.US, "%.0f%%", completedPercent * 100),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.text
                            )
                            Text(
                                text = "done",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                color = colors.muted
                            )
                        } else {
                            Text(
                                text = "$nextMinutesLeft",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.text
                            )
                            Text(
                                text = "min",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                color = colors.muted
                            )
                        }
                    }
                }
            }
        }
    }
}
