package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.CameraScreen
import com.example.ui.screens.FullPhotoViewScreen
import com.example.ui.screens.GalleryScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.PermissionsScreen
import com.example.ui.screens.PreviewScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.BorderOutline
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.SiteCamTheme
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.viewmodel.AppScreen
import com.example.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SiteCamTheme {
                val appViewModel: AppViewModel = viewModel()
                val currentScreen by appViewModel.currentScreen.collectAsStateWithLifecycle()
                val currentUser by appViewModel.currentUser.collectAsStateWithLifecycle()
                val isSigningIn by appViewModel.isSigningIn.collectAsStateWithLifecycle()
                val loginError by appViewModel.loginError.collectAsStateWithLifecycle()
                val hasCameraPermission by appViewModel.hasCameraPermission.collectAsStateWithLifecycle()
                val hasLocationPermission by appViewModel.hasLocationPermission.collectAsStateWithLifecycle()
                val settings by appViewModel.settings.collectAsStateWithLifecycle()
                val gpsData by appViewModel.locationTracker.gpsState.collectAsStateWithLifecycle()

                val siteLocation by appViewModel.siteLocationInput.collectAsStateWithLifecycle()
                val allEngineersLocations by appViewModel.allEngineersLocations.collectAsStateWithLifecycle()
                val projectNote by appViewModel.projectNoteInput.collectAsStateWithLifecycle()
                val recentLocations by appViewModel.recentLocations.collectAsStateWithLifecycle()
                val torchEnabled by appViewModel.torchEnabled.collectAsStateWithLifecycle()
                val useFrontCamera by appViewModel.useFrontCamera.collectAsStateWithLifecycle()
                val isCapturing by appViewModel.isCapturing.collectAsStateWithLifecycle()
                val captureError by appViewModel.captureErrorMessage.collectAsStateWithLifecycle()

                val pendingCapture by appViewModel.pendingCapture.collectAsStateWithLifecycle()
                val photos by appViewModel.photos.collectAsStateWithLifecycle()
                val selectedIds by appViewModel.selectedPhotoIds.collectAsStateWithLifecycle()
                val siteFilter by appViewModel.siteFilter.collectAsStateWithLifecycle()
                val activeFullPhoto by appViewModel.activeFullPhoto.collectAsStateWithLifecycle()
                val exportMessage by appViewModel.exportMessage.collectAsStateWithLifecycle()

                val context = LocalContext.current

                // Check initial permissions state on mount
                LaunchedEffect(Unit) {
                    val camera = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                    val fineLocation = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    val coarseLocation = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    appViewModel.updatePermissions(camera, fineLocation || coarseLocation)
                }

                // Global BackHandler
                BackHandler(enabled = currentScreen != AppScreen.CAMERA && currentScreen != AppScreen.LOGIN && currentScreen != AppScreen.SPLASH) {
                    appViewModel.goBack()
                }

                Crossfade(
                    targetState = currentScreen,
                    label = "screen_transition"
                ) { screen ->
                    when (screen) {
                        AppScreen.SPLASH -> {
                            SplashScreen()
                        }

                        AppScreen.LOGIN -> {
                            LoginScreen(
                                isSigningIn = isSigningIn,
                                errorMessage = loginError,
                                onGoogleSignInSuccess = { name, email, photo ->
                                    appViewModel.signInWithGoogleCredentials(name, email, photo)
                                },
                                onTestModeSignIn = {
                                    appViewModel.signInWithTestMode()
                                },
                                onError = { msg ->
                                    appViewModel.setLoginError(msg)
                                }
                            )
                        }

                        AppScreen.PERMISSIONS -> {
                            PermissionsScreen(
                                hasCameraPermission = hasCameraPermission,
                                hasLocationPermission = hasLocationPermission,
                                onPermissionsUpdated = { cam, loc ->
                                    appViewModel.updatePermissions(cam, loc)
                                }
                            )
                        }

                        AppScreen.PREVIEW -> {
                            pendingCapture?.let { photoState ->
                                PreviewScreen(
                                    capturedPhoto = photoState,
                                    onSavePhoto = {
                                        appViewModel.savePendingPhoto(context)
                                    },
                                    onRetakePhoto = {
                                        appViewModel.retakePendingPhoto()
                                    }
                                )
                            } ?: run {
                                appViewModel.navigateTo(AppScreen.CAMERA, clearStack = true)
                            }
                        }

                        AppScreen.FULL_PHOTO -> {
                            activeFullPhoto?.let { photo ->
                                FullPhotoViewScreen(
                                    photo = photo,
                                    onBack = { appViewModel.goBack() },
                                    onDeletePhoto = { toDelete ->
                                        appViewModel.deletePhoto(toDelete)
                                    }
                                )
                            } ?: run {
                                appViewModel.goBack()
                            }
                        }

                        AppScreen.CAMERA, AppScreen.GALLERY, AppScreen.SETTINGS -> {
                            val activeUser = currentUser
                            if (activeUser == null) {
                                LoginScreen(
                                    isSigningIn = isSigningIn,
                                    errorMessage = loginError,
                                    onGoogleSignInSuccess = { name, email, photo ->
                                        appViewModel.signInWithGoogleCredentials(name, email, photo)
                                    },
                                    onTestModeSignIn = { appViewModel.signInWithTestMode() },
                                    onError = { msg -> appViewModel.setLoginError(msg) }
                                )
                            } else {
                                Scaffold(
                                    bottomBar = {
                                        NavigationBar(
                                            containerColor = NavyDark,
                                            contentColor = TextWhite,
                                            modifier = Modifier
                                                .windowInsetsPadding(WindowInsets.navigationBars)
                                                .testTag("main_navigation_bar"),
                                            tonalElevation = 8.dp
                                        ) {
                                            // 1. Camera Item
                                            NavigationBarItem(
                                                selected = screen == AppScreen.CAMERA,
                                                onClick = {
                                                    appViewModel.navigateTo(AppScreen.CAMERA, clearStack = true)
                                                },
                                                icon = {
                                                    Icon(
                                                        imageVector = Icons.Default.PhotoCamera,
                                                        contentDescription = "Camera",
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                },
                                                label = {
                                                    Text(
                                                        "Camera",
                                                        fontSize = 11.sp,
                                                        fontWeight = if (screen == AppScreen.CAMERA) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                },
                                                colors = NavigationBarItemDefaults.colors(
                                                    selectedIconColor = NavyDark,
                                                    selectedTextColor = AmberAccent,
                                                    indicatorColor = AmberAccent,
                                                    unselectedIconColor = TextMuted,
                                                    unselectedTextColor = TextMuted
                                                ),
                                                modifier = Modifier.testTag("nav_camera")
                                            )

                                            // 2. Gallery Item
                                            NavigationBarItem(
                                                selected = screen == AppScreen.GALLERY,
                                                onClick = {
                                                    appViewModel.navigateTo(AppScreen.GALLERY, clearStack = true)
                                                },
                                                icon = {
                                                    BadgedBox(badge = {
                                                        if (photos.isNotEmpty()) {
                                                            Badge(
                                                                containerColor = AmberAccent,
                                                                contentColor = NavyDark
                                                            ) {
                                                                Text("${photos.size}")
                                                            }
                                                        }
                                                    }) {
                                                        Icon(
                                                            imageVector = Icons.Default.PhotoLibrary,
                                                            contentDescription = "Gallery",
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                    }
                                                },
                                                label = {
                                                    Text(
                                                        "Gallery",
                                                        fontSize = 11.sp,
                                                        fontWeight = if (screen == AppScreen.GALLERY) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                },
                                                colors = NavigationBarItemDefaults.colors(
                                                    selectedIconColor = NavyDark,
                                                    selectedTextColor = AmberAccent,
                                                    indicatorColor = AmberAccent,
                                                    unselectedIconColor = TextMuted,
                                                    unselectedTextColor = TextMuted
                                                ),
                                                modifier = Modifier.testTag("nav_gallery")
                                            )

                                            // 3. Settings Item
                                            NavigationBarItem(
                                                selected = screen == AppScreen.SETTINGS,
                                                onClick = {
                                                    appViewModel.navigateTo(AppScreen.SETTINGS, clearStack = true)
                                                },
                                                icon = {
                                                    Icon(
                                                        imageVector = Icons.Default.Settings,
                                                        contentDescription = "Settings",
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                },
                                                label = {
                                                    Text(
                                                        "Settings",
                                                        fontSize = 11.sp,
                                                        fontWeight = if (screen == AppScreen.SETTINGS) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                },
                                                colors = NavigationBarItemDefaults.colors(
                                                    selectedIconColor = NavyDark,
                                                    selectedTextColor = AmberAccent,
                                                    indicatorColor = AmberAccent,
                                                    unselectedIconColor = TextMuted,
                                                    unselectedTextColor = TextMuted
                                                ),
                                                modifier = Modifier.testTag("nav_settings")
                                            )
                                        }
                                    },
                                    containerColor = NavyPrimary,
                                    modifier = Modifier.fillMaxSize()
                                ) { innerPadding ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(innerPadding)
                                    ) {
                                        when (screen) {
                                            AppScreen.CAMERA -> {
                                                CameraScreen(
                                                    user = activeUser,
                                                    settings = settings,
                                                    gpsData = gpsData,
                                                    siteLocation = siteLocation,
                                                    projectNote = projectNote,
                                                    recentLocations = recentLocations,
                                                    torchEnabled = torchEnabled,
                                                    useFrontCamera = useFrontCamera,
                                                    isCapturing = isCapturing,
                                                    errorMessage = captureError,
                                                    onSiteLocationChange = { appViewModel.updateSiteLocation(it) },
                                                    onProjectNoteChange = { appViewModel.updateProjectNote(it) },
                                                    onToggleTorch = { appViewModel.toggleTorch() },
                                                    onToggleCameraFacing = { appViewModel.toggleCameraFacing() },
                                                    onCaptureTriggered = { capture, ctx ->
                                                        appViewModel.triggerCapture(capture, ctx)
                                                    },
                                                    onDismissError = { appViewModel.dismissCaptureError() },
                                                    onToggleMockGps = { enabled ->
                                                        appViewModel.updateSettings(settings.copy(mockGpsEnabled = enabled))
                                                    }
                                                )
                                            }

                                            AppScreen.GALLERY -> {
                                                GalleryScreen(
                                                    photos = photos,
                                                    selectedIds = selectedIds,
                                                    siteFilter = siteFilter,
                                                    exportMessage = exportMessage,
                                                    onSiteFilterChange = { appViewModel.setSiteFilter(it) },
                                                    onPhotoClick = { appViewModel.viewFullPhoto(it) },
                                                    onToggleSelect = { appViewModel.togglePhotoSelection(it) },
                                                    onSelectAll = { appViewModel.selectAllPhotos() },
                                                    onClearSelection = { appViewModel.clearPhotoSelection() },
                                                    onDeleteSelected = { appViewModel.deleteSelectedPhotos() },
                                                    onExportZip = { appViewModel.exportSelectedAsZip(it) },
                                                    onDismissExportMessage = { appViewModel.dismissExportMessage() },
                                                    onNavigateToCamera = {
                                                        appViewModel.navigateTo(AppScreen.CAMERA, clearStack = true)
                                                    }
                                                )
                                            }

                                            AppScreen.SETTINGS -> {
                                                SettingsScreen(
                                                    user = activeUser,
                                                    settings = settings,
                                                    siteLocation = siteLocation,
                                                    allEngineersLocations = allEngineersLocations,
                                                    onSiteLocationChange = { appViewModel.updateSiteLocation(it) },
                                                    onUpdateProfile = { name, phone ->
                                                        appViewModel.updateUserProfile(name, phone)
                                                    },
                                                    onSettingsUpdated = { appViewModel.updateSettings(it) },
                                                    onSignOut = { appViewModel.signOut() },
                                                    onClearAllData = { appViewModel.clearAllUserData(it) }
                                                )
                                            }

                                            else -> {}
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
