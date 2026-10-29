package com.example.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.export.ExportManager
import com.example.model.SitePhoto
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.BorderOutline
import com.example.ui.theme.GpsRed
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavySurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import java.io.File

@Composable
fun FullPhotoViewScreen(
    photo: SitePhoto,
    onBack: () -> Unit,
    onDeletePhoto: (SitePhoto) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    BackHandler {
        onBack()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("full_photo_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NavyDark)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextWhite
                    )
                }

                Text(
                    text = photo.fileName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextWhite,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )

                Row {
                    IconButton(onClick = {
                        ExportManager.shareSinglePhoto(context, photo)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = AmberAccent
                        )
                    }

                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = GpsRed
                        )
                    }
                }
            }

            // Photo Viewer (Takes proportional space)
            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxWidth()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(File(photo.filePath))
                        .crossfade(true)
                        .build(),
                    contentDescription = photo.siteLocation,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Metadata Detail Card
            Card(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Proof Verification Details",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberAccent
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    MetadataRow("Site Location", photo.siteLocation)
                    photo.gpsLocationName?.let {
                        if (it.isNotBlank()) {
                            MetadataRow("GPS Location Name", it)
                        }
                    }
                    MetadataRow("GPS Coordinates", "${photo.latitude ?: "N/A"}, ${photo.longitude ?: "N/A"}")
                    MetadataRow("Accuracy", photo.accuracyMeters?.let { "±${String.format(java.util.Locale.US, "%.1f", it)} meters" } ?: "N/A")
                    MetadataRow("Timestamp", photo.formattedDateTime)
                    MetadataRow("Captured By", photo.userName)
                    MetadataRow("Engineer Email", photo.userEmail)
                    if (photo.projectNote.isNotBlank()) {
                        MetadataRow("Project / Note", photo.projectNote)
                    }
                    MetadataRow("Timezone", photo.timezone)
                    MetadataRow("File Path", photo.filePath)
                }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Proof Photo", color = TextWhite) },
                text = { Text("Permanently delete this photo from the device? This action cannot be reversed.", color = Color(0xFFCBD5E1)) },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        onDeletePhoto(photo)
                    }) {
                        Text("Delete", color = GpsRed, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel", color = TextMuted)
                    }
                },
                containerColor = NavyDark,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            letterSpacing = 0.5.sp
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = TextWhite,
            fontWeight = FontWeight.Medium
        )
        HorizontalDivider(
            color = BorderOutline.copy(alpha = 0.5f),
            thickness = 0.5.dp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
