package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Note
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.camera.WatermarkProcessor
import com.example.model.AppSettings
import com.example.model.GpsData
import com.example.model.GpsStatus
import com.example.model.UserSession
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.BorderOutline
import com.example.ui.theme.GpsGreen
import com.example.ui.theme.GpsRed
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavySurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import kotlinx.coroutines.delay

@Composable
fun GpsStatusChip(
    gpsData: GpsData,
    modifier: Modifier = Modifier
) {
    val (statusText, statusColor) = when (gpsData.status) {
        GpsStatus.LOCKED -> {
            val acc = gpsData.accuracyMeters?.let { " (±${String.format(java.util.Locale.US, "%.0f", it)} m)" } ?: ""
            "GPS locked$acc" to GpsGreen
        }
        GpsStatus.WEAK -> {
            val acc = gpsData.accuracyMeters?.let { " (±${String.format(java.util.Locale.US, "%.0f", it)} m)" } ?: ""
            "Weak GPS$acc" to AmberAccent
        }
        GpsStatus.WAITING -> "Waiting for GPS..." to GpsRed
        GpsStatus.UNAVAILABLE -> "GPS unavailable" to TextMuted
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(NavyDark.copy(alpha = 0.85f))
            .border(1.dp, statusColor.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .testTag("gps_status_chip")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Text(
                text = statusText,
                color = TextWhite,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun SiteLocationHeader(
    siteLocation: String,
    projectNote: String,
    recentLocations: List<String>,
    onSiteLocationChange: (String) -> Unit,
    onProjectNoteChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("site_location_header"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = NavyDark.copy(alpha = 0.90f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderOutline))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Main site location input bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = AmberAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SITE LOCATION",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberAccent,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = if (siteLocation.isNotBlank()) siteLocation else "Tap to set site location...",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextWhite,
                        maxLines = 1
                    )
                }
                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.ArrowDropDown,
                        contentDescription = "Expand Location Picker",
                        tint = AmberAccent
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    OutlinedTextField(
                        value = siteLocation,
                        onValueChange = onSiteLocationChange,
                        label = { Text("Site Location (e.g. Plot 14, Solar Plant)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("site_location_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberAccent,
                            unfocusedBorderColor = BorderOutline,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedLabelColor = AmberAccent,
                            unfocusedLabelColor = TextMuted
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = projectNote,
                        onValueChange = onProjectNoteChange,
                        label = { Text("Project / Note (Optional)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("project_note_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberAccent,
                            unfocusedBorderColor = BorderOutline,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedLabelColor = AmberAccent,
                            unfocusedLabelColor = TextMuted
                        )
                    )

                    if (recentLocations.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Recent Sites (Quick Pick):",
                            fontSize = 11.sp,
                            color = TextMuted,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            recentLocations.forEach { loc ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(NavySurface)
                                        .border(1.dp, BorderOutline, RoundedCornerShape(12.dp))
                                        .clickable {
                                            onSiteLocationChange(loc)
                                            isExpanded = false
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = loc,
                                        fontSize = 11.sp,
                                        color = if (loc == siteLocation) AmberAccent else TextWhite,
                                        fontWeight = if (loc == siteLocation) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LiveWatermarkOverlay(
    siteLocation: String,
    projectNote: String,
    gpsData: GpsData,
    user: UserSession,
    settings: AppSettings,
    modifier: Modifier = Modifier
) {
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Live real-time clock ticker (updates every second)
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val displaySite = if (siteLocation.isBlank()) "Site: Tap top bar to set location" else siteLocation.trim()
    val coordText = WatermarkProcessor.formatCoordinates(gpsData.latitude, gpsData.longitude, gpsData.accuracyMeters)
    val timeText = WatermarkProcessor.formatDateTime(currentTimeMillis, settings.timezone, settings.dateFormat)

    val isTop = settings.watermarkPosition.equals("TOP", ignoreCase = true)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("live_watermark_overlay")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(NavyPrimary.copy(alpha = settings.watermarkOpacity.coerceIn(0.2f, 0.95f)))
        ) {
            // Amber accent divider
            if (!isTop) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(AmberAccent)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                // Line 1: Site Name
                Text(
                    text = "SITE: $displaySite",
                    fontSize = when (settings.textSize) {
                        "SMALL" -> 13.sp
                        "LARGE" -> 17.sp
                        else -> 15.sp
                    },
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Line 2: GPS
                Text(
                    text = "GPS: $coordText",
                    fontSize = when (settings.textSize) {
                        "SMALL" -> 11.sp
                        "LARGE" -> 14.sp
                        else -> 12.sp
                    },
                    color = Color(0xFFE2E8F0)
                )

                // Line 3: GPS Location Name
                val locName = gpsData.locationName?.trim()
                    ?: if (gpsData.latitude != null && gpsData.longitude != null) {
                        com.example.data.location.LocationTracker.getApproximateRegion(gpsData.latitude, gpsData.longitude)
                    } else null

                if (!locName.isNullOrBlank()) {
                    Text(
                        text = "GPS LOCATION: $locName",
                        fontSize = when (settings.textSize) {
                            "SMALL" -> 11.sp
                            "LARGE" -> 14.sp
                            else -> 12.sp
                        },
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFFEF08A)
                    )
                }

                // Line 4: Timestamp
                Text(
                    text = "TIME: $timeText",
                    fontSize = when (settings.textSize) {
                        "SMALL" -> 11.sp
                        "LARGE" -> 14.sp
                        else -> 12.sp
                    },
                    color = Color(0xFFE2E8F0)
                )

                // Line 5: Captured by
                if (settings.showCapturedBy) {
                    val phoneSuffix = if (user.phoneNumber.isNotBlank()) " (${user.phoneNumber})" else ""
                    Text(
                        text = "Captured by: ${user.displayName}$phoneSuffix",
                        fontSize = when (settings.textSize) {
                            "SMALL" -> 10.sp
                            "LARGE" -> 13.sp
                            else -> 11.sp
                        },
                        color = Color(0xFFCBD5E1)
                    )
                }

                // Line 5: Project note
                if (projectNote.isNotBlank()) {
                    Text(
                        text = "Project / Note: $projectNote",
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic,
                        color = Color(0xFFFDE68A)
                    )
                }
            }

            if (isTop) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(AmberAccent)
                )
            }
        }
    }
}
