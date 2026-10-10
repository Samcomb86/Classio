package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AppTheme

@Composable
fun FreeGapCard(
    start: String,
    end: String,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        color = colors.surface2,
        border = BorderStroke(1.dp, colors.line)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.HourglassEmpty,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = colors.muted
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Free Slot: $start to $end",
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun PrayerBreakCard(
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag("prayer_break_card"),
        shape = RoundedCornerShape(12.dp),
        color = colors.surface2,
        border = BorderStroke(1.dp, colors.line)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Mosque,
                contentDescription = "Prayer break",
                modifier = Modifier.size(18.dp),
                tint = colors.muted
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Zuhr Prayer & Lunch Break",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.text
                )
                Text(
                    text = "12:50 PM - 01:10 PM",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted
                )
            }
        }
    }
}

@Composable
fun EmptyProgramTimetableCard(
    programName: String,
    onPickOtherClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("empty_timetable_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.line)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(colors.primarySoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EventBusy,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = colors.primary
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Timetable Not Published Yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colors.text,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Timetable for $programName has not been published yet. We will load it automatically when the university shares it.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = colors.muted
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onPickOtherClick,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("change_class_empty_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary
                )
            ) {
                Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select Another Program", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
