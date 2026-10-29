package com.example.ui.screens

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.model.AppSettings
import com.example.model.GpsData
import com.example.model.GpsStatus
import com.example.model.UserSession
import com.example.ui.components.GpsStatusChip
import com.example.ui.components.LiveWatermarkOverlay
import com.example.ui.components.SiteLocationHeader
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.BorderOutline
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@Composable
fun CameraScreen(
    user: UserSession,
    settings: AppSettings,
    gpsData: GpsData,
    siteLocation: String,
    projectNote: String,
    recentLocations: List<String>,
    torchEnabled: Boolean,
    useFrontCamera: Boolean,
    isCapturing: Boolean,
    errorMessage: String?,
    onSiteLocationChange: (String) -> Unit,
    onProjectNoteChange: (String) -> Unit,
    onToggleTorch: () -> Unit,
    onToggleCameraFacing: () -> Unit,
    onCaptureTriggered: (ImageCapture?, Context) -> Unit,
    onDismissError: () -> Unit,
    onToggleMockGps: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var cameraControl: CameraControl? by remember { mutableStateOf(null) }
    var cameraAvailable by remember { mutableStateOf(true) }

    // Persistent PreviewView with COMPATIBLE (TextureView) implementation mode
    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    // Bind Camera once per lifecycleOwner and camera lens facing
    LaunchedEffect(lifecycleOwner, useFrontCamera) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val provider = cameraProviderFuture.get()

                val hasBack = provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
                val hasFront = provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)

                val selector = when {
                    useFrontCamera && hasFront -> CameraSelector.DEFAULT_FRONT_CAMERA
                    hasBack -> CameraSelector.DEFAULT_BACK_CAMERA
                    hasFront -> CameraSelector.DEFAULT_FRONT_CAMERA
                    else -> CameraSelector.DEFAULT_BACK_CAMERA
                }

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .build()
                imageCapture = capture

                provider.unbindAll()
                val boundCamera: Camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview,
                    capture
                )
                cameraControl = boundCamera.cameraControl
                cameraAvailable = true
            } catch (e: Exception) {
                Log.e("CameraScreen", "Failed to bind camera: ${e.message}", e)
                cameraAvailable = false
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // Handle torch toggle without re-binding camera
    LaunchedEffect(torchEnabled, cameraControl) {
        try {
            cameraControl?.enableTorch(torchEnabled)
        } catch (_: Exception) {}
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("camera_screen")
    ) {
        // 1. Camera Viewfinder
        if (cameraAvailable) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            FieldViewfinderFallback(siteLocation = siteLocation)
        }

        // 2. Top Bar & Location Inputs (Floating above preview)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .align(Alignment.TopCenter)
        ) {
            SiteLocationHeader(
                siteLocation = siteLocation,
                projectNote = projectNote,
                recentLocations = recentLocations,
                onSiteLocationChange = onSiteLocationChange,
                onProjectNoteChange = onProjectNoteChange
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Status Bar Row: GPS Chip + Mock GPS Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GpsStatusChip(gpsData = gpsData)

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (settings.mockGpsEnabled) AmberAccent else NavyDark.copy(alpha = 0.85f))
                        .border(1.dp, AmberAccent, RoundedCornerShape(12.dp))
                        .clickable { onToggleMockGps(!settings.mockGpsEnabled) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (settings.mockGpsEnabled) "Mock GPS: ON" else "Mock GPS",
                        fontSize = 10.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = if (settings.mockGpsEnabled) NavyDark else AmberAccent
                    )
                }
            }
        }

        // 3. Live Watermark Overlay (Placed at bottom by default)
        val isTop = settings.watermarkPosition.equals("TOP", ignoreCase = true)
        LiveWatermarkOverlay(
            siteLocation = siteLocation,
            projectNote = projectNote,
            gpsData = gpsData,
            user = user,
            settings = settings,
            modifier = Modifier
                .align(if (isTop) Alignment.TopCenter else Alignment.BottomCenter)
                .padding(
                    top = if (isTop) 130.dp else 0.dp,
                    bottom = if (!isTop) 110.dp else 0.dp
                )
        )

        // 4. Shutter & Camera Controls Row (Floating at bottom above nav bar)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp, start = 24.dp, end = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flash Toggle
            IconButton(
                onClick = onToggleTorch,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(NavyDark.copy(alpha = 0.75f))
                    .border(1.dp, BorderOutline, CircleShape)
            ) {
                Icon(
                    imageVector = if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Toggle Torch",
                    tint = if (torchEnabled) AmberAccent else TextWhite,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Big Shutter Button
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .border(4.dp, AmberAccent, CircleShape)
                    .background(Color.Transparent)
                    .clickable(enabled = !isCapturing) {
                        onCaptureTriggered(imageCapture, context)
                    }
                    .padding(6.dp)
                    .testTag("shutter_button"),
                contentAlignment = Alignment.Center
            ) {
                if (isCapturing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        color = AmberAccent,
                        strokeWidth = 3.dp
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }

            // Flip Camera (Front / Rear)
            IconButton(
                onClick = onToggleCameraFacing,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(NavyDark.copy(alpha = 0.75f))
                    .border(1.dp, BorderOutline, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.FlipCameraAndroid,
                    contentDescription = "Flip Camera",
                    tint = TextWhite,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // GPS Block Alert Dialog
        if (errorMessage != null) {
            AlertDialog(
                onDismissRequest = onDismissError,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = AmberAccent
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("GPS Fix Required", color = TextWhite)
                    }
                },
                text = {
                    Text(errorMessage, color = Color(0xFFCBD5E1), fontSize = 14.sp)
                },
                confirmButton = {
                    TextButton(onClick = {
                        onToggleMockGps(true)
                        onDismissError()
                    }) {
                        Text("Enable Mock GPS", color = AmberAccent, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissError) {
                        Text("Dismiss", color = TextMuted)
                    }
                },
                containerColor = NavyDark,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
private fun FieldViewfinderFallback(siteLocation: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1E33)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                tint = AmberAccent.copy(alpha = 0.7f),
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Live Camera Viewfinder",
                color = TextWhite,
                fontSize = 16.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tap shutter button to capture and burn watermark",
                color = TextMuted,
                fontSize = 12.sp
            )
        }
    }
}
