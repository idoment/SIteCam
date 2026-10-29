package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.export.ExportManager
import com.example.model.SitePhoto
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.BorderOutline
import com.example.ui.theme.GpsGreen
import com.example.ui.theme.GpsRed
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavySurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(
    photos: List<SitePhoto>,
    selectedIds: Set<Long>,
    siteFilter: String?,
    exportMessage: String?,
    onSiteFilterChange: (String?) -> Unit,
    onPhotoClick: (SitePhoto) -> Unit,
    onToggleSelect: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    onExportZip: (Context) -> Unit,
    onDismissExportMessage: () -> Unit,
    onNavigateToCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Distinct sites for filtering
    val distinctSites = remember(photos) {
        photos.map { it.siteLocation }.distinct()
    }

    val filteredPhotos = remember(photos, siteFilter) {
        if (siteFilter == null) photos else photos.filter { it.siteLocation == siteFilter }
    }

    val isSelectionMode = selectedIds.isNotEmpty()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NavyPrimary)
            .statusBarsPadding()
            .testTag("gallery_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelectionMode) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onClearSelection) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Selection", tint = TextWhite)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${selectedIds.size} selected",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onSelectAll) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All", tint = AmberAccent)
                        }
                        IconButton(onClick = {
                            val selectedList = photos.filter { selectedIds.contains(it.id) }
                            ExportManager.shareMultiplePhotos(context, selectedList)
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share Selected", tint = AmberAccent)
                        }
                        IconButton(onClick = { onExportZip(context) }) {
                            Icon(Icons.Default.Archive, contentDescription = "Export ZIP", tint = AmberAccent)
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Selected", tint = GpsRed)
                        }
                    }
                } else {
                    Column {
                        Text(
                            text = "Site Proof Gallery",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = "${photos.size} verified photos on device",
                            fontSize = 12.sp,
                            color = AmberAccent
                        )
                    }

                    if (photos.isNotEmpty()) {
                        Button(
                            onClick = { onExportZip(context) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NavySurface,
                                contentColor = AmberAccent
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(AmberAccent)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Archive,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = AmberAccent
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export ZIP", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Site Filter Chips (Horizontal Scroll)
            if (distinctSites.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        label = "All Sites (${photos.size})",
                        isSelected = siteFilter == null,
                        onClick = { onSiteFilterChange(null) }
                    )
                    distinctSites.forEach { site ->
                        val count = photos.count { it.siteLocation == site }
                        FilterChip(
                            label = "$site ($count)",
                            isSelected = siteFilter == site,
                            onClick = { onSiteFilterChange(site) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Content: Empty State vs Photo Grid
            if (filteredPhotos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(NavySurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = AmberAccent,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (siteFilter != null) "No photos for this site" else "No Site Photos Captured Yet",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Switch to the Camera tab to take proof photos with live GPS and time stamps.",
                            fontSize = 13.sp,
                            color = TextMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onNavigateToCamera,
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AmberAccent,
                                contentColor = NavyDark
                            )
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Camera", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 90.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredPhotos, key = { it.id }) { photo ->
                        val isSelected = selectedIds.contains(photo.id)
                        PhotoGridItem(
                            photo = photo,
                            isSelected = isSelected,
                            isSelectionMode = isSelectionMode,
                            onClick = {
                                if (isSelectionMode) {
                                    onToggleSelect(photo.id)
                                } else {
                                    onPhotoClick(photo)
                                }
                            },
                            onLongClick = {
                                onToggleSelect(photo.id)
                            }
                        )
                    }
                }
            }
        }

        // Export Status Alert
        if (exportMessage != null) {
            AlertDialog(
                onDismissRequest = onDismissExportMessage,
                title = { Text("Export Status", color = TextWhite) },
                text = { Text(exportMessage, color = Color(0xFFCBD5E1)) },
                confirmButton = {
                    TextButton(onClick = onDismissExportMessage) {
                        Text("OK", color = AmberAccent, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = NavyDark,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Delete Confirmation Dialog
        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete Photos", color = TextWhite) },
                text = {
                    Text(
                        "Are you sure you want to permanently delete ${selectedIds.size} selected photo(s)? This cannot be undone.",
                        color = Color(0xFFCBD5E1)
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteConfirm = false
                        onDeleteSelected()
                    }) {
                        Text("Delete", color = GpsRed, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Cancel", color = TextMuted)
                    }
                },
                containerColor = NavyDark,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoGridItem(
    photo: SitePhoto,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) AmberAccent else BorderOutline,
                shape = RoundedCornerShape(14.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("photo_item_${photo.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = NavyDark)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(File(photo.filePath))
                    .crossfade(true)
                    .build(),
                contentDescription = photo.siteLocation,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Dark gradient overlay for text legibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0x60000000),
                                Color(0xE00B1F3A)
                            ),
                            startY = 100f
                        )
                    )
            )

            // Selection indicator badge
            if (isSelectionMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) AmberAccent else NavyDark.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isSelected) NavyDark else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Bottom metadata label
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = photo.siteLocation,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = photo.formattedDateTime,
                    fontSize = 10.sp,
                    color = AmberAccent,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) AmberAccent else NavySurface)
            .border(1.dp, if (isSelected) AmberAccent else BorderOutline, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) NavyDark else TextWhite
        )
    }
}
