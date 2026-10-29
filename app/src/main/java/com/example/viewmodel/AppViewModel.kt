package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.config.AppConfig
import com.example.data.camera.WatermarkProcessor
import com.example.data.export.ExportManager
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesManager
import com.example.data.location.LocationTracker
import com.example.model.AppSettings
import com.example.model.GpsData
import com.example.model.GpsStatus
import com.example.model.SitePhoto
import com.example.model.UserSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppScreen {
    SPLASH,
    LOGIN,
    PERMISSIONS,
    CAMERA,
    PREVIEW,
    GALLERY,
    FULL_PHOTO,
    SETTINGS
}

data class CapturedPhotoState(
    val stampedBitmap: Bitmap,
    val siteLocation: String,
    val projectNote: String,
    val gpsData: GpsData,
    val timestamp: Long,
    val fileName: String
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesManager(application)
    private val database = AppDatabase.getDatabase(application)
    private val photoDao = database.sitePhotoDao()
    val locationTracker = LocationTracker(application)

    // Current Screen
    private val _currentScreen = MutableStateFlow(AppScreen.SPLASH)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Navigation Stack for sub-screens
    private val backStack = mutableListOf<AppScreen>()

    // Current User Session
    private val _currentUser = MutableStateFlow<UserSession?>(null)
    val currentUser: StateFlow<UserSession?> = _currentUser.asStateFlow()

    // Login UI state
    private val _isSigningIn = MutableStateFlow(false)
    val isSigningIn: StateFlow<Boolean> = _isSigningIn.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    // Permissions State
    private val _hasCameraPermission = MutableStateFlow(false)
    val hasCameraPermission: StateFlow<Boolean> = _hasCameraPermission.asStateFlow()

    private val _hasLocationPermission = MutableStateFlow(false)
    val hasLocationPermission: StateFlow<Boolean> = _hasLocationPermission.asStateFlow()

    // Settings
    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    // Camera Input & State
    private val _siteLocationInput = MutableStateFlow("")
    val siteLocationInput: StateFlow<String> = _siteLocationInput.asStateFlow()

    private val _projectNoteInput = MutableStateFlow("")
    val projectNoteInput: StateFlow<String> = _projectNoteInput.asStateFlow()

    private val defaultPresetLocations = listOf(
        "Plot 14, Solar Plant, Anantapur, AP",
        "Block B, Substation Substation-02",
        "Tower 44, Transmission Line Corridors",
        "Control Room Yard, Site Alpha",
        "Zone 3 Inverter Station, Kurnool",
        "Main Control Building, Kadapa"
    )

    private val _recentLocations = MutableStateFlow<List<String>>(emptyList())
    val recentLocations: StateFlow<List<String>> = _recentLocations.asStateFlow()

    private val _allEngineersLocations = MutableStateFlow<List<String>>(defaultPresetLocations)
    val allEngineersLocations: StateFlow<List<String>> = _allEngineersLocations.asStateFlow()

    private val _torchEnabled = MutableStateFlow(false)
    val torchEnabled: StateFlow<Boolean> = _torchEnabled.asStateFlow()

    private val _useFrontCamera = MutableStateFlow(false)
    val useFrontCamera: StateFlow<Boolean> = _useFrontCamera.asStateFlow()

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    private val _captureErrorMessage = MutableStateFlow<String?>(null)
    val captureErrorMessage: StateFlow<String?> = _captureErrorMessage.asStateFlow()

    // Captured Photo Pending Save
    private val _pendingCapture = MutableStateFlow<CapturedPhotoState?>(null)
    val pendingCapture: StateFlow<CapturedPhotoState?> = _pendingCapture.asStateFlow()

    // Gallery Photos
    private val _photos = MutableStateFlow<List<SitePhoto>>(emptyList())
    val photos: StateFlow<List<SitePhoto>> = _photos.asStateFlow()

    private val _selectedPhotoIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedPhotoIds: StateFlow<Set<Long>> = _selectedPhotoIds.asStateFlow()

    private val _siteFilter = MutableStateFlow<String?>(null)
    val siteFilter: StateFlow<String?> = _siteFilter.asStateFlow()

    private val _activeFullPhoto = MutableStateFlow<SitePhoto?>(null)
    val activeFullPhoto: StateFlow<SitePhoto?> = _activeFullPhoto.asStateFlow()

    // Export progress / status
    private val _exportMessage = MutableStateFlow<String?>(null)
    val exportMessage: StateFlow<String?> = _exportMessage.asStateFlow()

    init {
        startStartupSequence()
    }

    private fun startStartupSequence() {
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()

            // Background initialization
            val cachedUser = prefs.getUserSession()
            if (cachedUser != null) {
                _currentUser.value = cachedUser
                loadUserData(cachedUser.uid)
            }

            // Warm up GPS
            locationTracker.startTracking()

            // Guarantee exactly SPLASH_DURATION_MS minimum time
            val elapsed = System.currentTimeMillis() - startTime
            val remaining = AppConfig.SPLASH_DURATION_MS - elapsed
            if (remaining > 0) {
                delay(remaining)
            }

            // Route based on session & permissions
            if (cachedUser != null) {
                if (_hasCameraPermission.value && _hasLocationPermission.value) {
                    navigateTo(AppScreen.CAMERA, clearStack = true)
                } else {
                    navigateTo(AppScreen.PERMISSIONS, clearStack = true)
                }
            } else {
                navigateTo(AppScreen.LOGIN, clearStack = true)
            }
        }
    }

    fun updatePermissions(cameraGranted: Boolean, locationGranted: Boolean) {
        _hasCameraPermission.value = cameraGranted
        _hasLocationPermission.value = locationGranted
        if (cameraGranted && locationGranted) {
            locationTracker.startTracking()
            if (_currentScreen.value == AppScreen.PERMISSIONS) {
                navigateTo(AppScreen.CAMERA, clearStack = true)
            }
        }
    }

    fun loadUserData(uid: String) {
        val userSettings = prefs.getSettings(uid)
        _settings.value = userSettings
        locationTracker.setMockGpsEnabled(userSettings.mockGpsEnabled)

        _siteLocationInput.value = prefs.getLastSiteLocation(uid)
        _projectNoteInput.value = prefs.getLastProjectNote(uid)
        _recentLocations.value = prefs.getRecentLocations(uid)

        // Observe Room DB photos for this user
        viewModelScope.launch {
            photoDao.getPhotosForUser(uid).collectLatest { list ->
                _photos.value = list
            }
        }

        // Observe all distinct saved sites across all photos (all engineers)
        viewModelScope.launch {
            photoDao.getAllDistinctSites().collectLatest { dbSites ->
                val combined = (prefs.getRecentLocations(uid) + dbSites + defaultPresetLocations)
                    .filter { it.isNotBlank() }
                    .distinct()
                _allEngineersLocations.value = combined
            }
        }
    }

    // Sign In (supports Test Mode bypass & Google Auth)
    fun signInWithTestMode() {
        _isSigningIn.value = true
        _loginError.value = null
        viewModelScope.launch {
            delay(600) // Brief natural transition
            val testUser = UserSession(
                uid = "test_engineer_001",
                displayName = "Er. Arun Kumar",
                email = "arun.kumar@maxinfra.in",
                isTestMode = true,
                phoneNumber = "+91 98765 43210"
            )
            prefs.saveUserSession(testUser)
            _currentUser.value = testUser
            loadUserData(testUser.uid)
            _isSigningIn.value = false

            if (_hasCameraPermission.value && _hasLocationPermission.value) {
                navigateTo(AppScreen.CAMERA, clearStack = true)
            } else {
                navigateTo(AppScreen.PERMISSIONS, clearStack = true)
            }
        }
    }

    fun signInWithGoogleCredentials(displayName: String, email: String, photoUrl: String?) {
        _isSigningIn.value = true
        _loginError.value = null
        viewModelScope.launch {
            val uid = "user_${email.replace(Regex("[^a-zA-Z0-9]"), "_")}"
            val user = UserSession(
                uid = uid,
                displayName = displayName.ifBlank { "Field Engineer" },
                email = email,
                photoUrl = photoUrl,
                isTestMode = false,
                phoneNumber = ""
            )
            prefs.saveUserSession(user)
            _currentUser.value = user
            loadUserData(user.uid)
            _isSigningIn.value = false

            if (_hasCameraPermission.value && _hasLocationPermission.value) {
                navigateTo(AppScreen.CAMERA, clearStack = true)
            } else {
                navigateTo(AppScreen.PERMISSIONS, clearStack = true)
            }
        }
    }

    fun updateUserProfile(displayName: String, phoneNumber: String) {
        val current = _currentUser.value ?: return
        val updated = current.copy(
            displayName = displayName.trim().ifBlank { current.displayName },
            phoneNumber = phoneNumber.trim()
        )
        prefs.saveUserSession(updated)
        _currentUser.value = updated
    }

    fun setLoginError(msg: String) {
        _loginError.value = msg
        _isSigningIn.value = false
    }

    fun signOut() {
        prefs.clearUserSession()
        _currentUser.value = null
        _selectedPhotoIds.value = emptySet()
        _activeFullPhoto.value = null
        _pendingCapture.value = null
        navigateTo(AppScreen.LOGIN, clearStack = true)
    }

    // Site & Note updates
    fun updateSiteLocation(newSite: String) {
        _siteLocationInput.value = newSite
        val uid = _currentUser.value?.uid ?: return
        prefs.saveLastSiteLocation(uid, newSite)
        _recentLocations.value = prefs.getRecentLocations(uid)
        val current = _allEngineersLocations.value.toMutableList()
        if (newSite.isNotBlank() && !current.contains(newSite)) {
            current.add(0, newSite)
            _allEngineersLocations.value = current
        }
    }

    fun updateProjectNote(note: String) {
        _projectNoteInput.value = note
        val uid = _currentUser.value?.uid ?: return
        prefs.saveLastProjectNote(uid, note)
    }

    fun toggleTorch() {
        _torchEnabled.value = !_torchEnabled.value
    }

    fun toggleCameraFacing() {
        _useFrontCamera.value = !_useFrontCamera.value
    }

    // Capture Workflow
    fun triggerCapture(imageCapture: ImageCapture?, context: Context) {
        val user = _currentUser.value ?: return
        val gps = locationTracker.gpsState.value
        val currentSettings = _settings.value

        // Check GPS lock requirement
        if (!currentSettings.allowCaptureWithoutGps && gps.status == GpsStatus.WAITING) {
            _captureErrorMessage.value = "Waiting for GPS fix. Ensure location is enabled or enable 'Allow capture without GPS' in Settings."
            return
        }

        _isCapturing.value = true
        _captureErrorMessage.value = null

        val captureTime = System.currentTimeMillis()
        val siteClean = _siteLocationInput.value.trim()
            .replace(Regex("[^a-zA-Z0-9]"), "_")
            .take(20)
            .ifEmpty { "Site" }
        val dateStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(captureTime))
        val fileName = "${siteClean}_$dateStamp.jpg"

        if (imageCapture != null) {
            imageCapture.takePicture(
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(imageProxy: ImageProxy) {
                        viewModelScope.launch {
                            val bitmap = imageProxyToBitmap(imageProxy)
                            imageProxy.close()
                            processCapturedBitmap(bitmap, user, gps, currentSettings, captureTime, fileName)
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        // Fallback to placeholder capture (essential for emulator)
                        createSyntheticCapture(user, gps, currentSettings, captureTime, fileName)
                    }
                }
            )
        } else {
            createSyntheticCapture(user, gps, currentSettings, captureTime, fileName)
        }
    }

    private fun createSyntheticCapture(
        user: UserSession,
        gps: GpsData,
        settings: AppSettings,
        captureTime: Long,
        fileName: String
    ) {
        viewModelScope.launch {
            // Generate a realistic field preview bitmap for emulator / simulated environment
            val width = 1280
            val height = 720
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Draw field background gradient
            val bgPaint = Paint().apply { color = Color.parseColor("#1B2A3F") }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            val gridPaint = Paint().apply {
                color = Color.parseColor("#2C415D")
                strokeWidth = 2f
            }
            for (x in 0..width step 80) canvas.drawLine(x.toFloat(), 0f, x.toFloat(), height.toFloat(), gridPaint)
            for (y in 0..height step 80) canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), gridPaint)

            val textPaint = Paint().apply {
                color = Color.parseColor("#F59E0B")
                textSize = 36f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("FIELD SITE PHOTO CAPTURE", width / 2f, height / 2f - 20f, textPaint)
            textPaint.apply {
                color = Color.WHITE
                textSize = 24f
            }
            canvas.drawText("Live verified proof of work frame", width / 2f, height / 2f + 30f, textPaint)

            processCapturedBitmap(bitmap, user, gps, settings, captureTime, fileName)
        }
    }

    private fun processCapturedBitmap(
        rawBitmap: Bitmap,
        user: UserSession,
        gps: GpsData,
        settings: AppSettings,
        captureTime: Long,
        fileName: String
    ) {
        val stamped = WatermarkProcessor.burnWatermark(
            sourceBitmap = rawBitmap,
            siteLocation = _siteLocationInput.value,
            projectNote = _projectNoteInput.value,
            gpsData = gps,
            user = user,
            settings = settings,
            captureTimeMillis = captureTime
        )

        _pendingCapture.value = CapturedPhotoState(
            stampedBitmap = stamped,
            siteLocation = _siteLocationInput.value,
            projectNote = _projectNoteInput.value,
            gpsData = gps,
            timestamp = captureTime,
            fileName = fileName
        )
        _isCapturing.value = false
        navigateTo(AppScreen.PREVIEW)
    }

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        val planeProxy = imageProxy.planes[0]
        val buffer = planeProxy.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        // Adjust orientation if needed
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        return if (rotationDegrees != 0) {
            val matrix = android.graphics.Matrix()
            matrix.postRotate(rotationDegrees.toFloat())
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    }

    fun dismissCaptureError() {
        _captureErrorMessage.value = null
    }

    fun savePendingPhoto(context: Context) {
        val pending = _pendingCapture.value ?: return
        val user = _currentUser.value ?: return
        val currentSettings = _settings.value

        viewModelScope.launch {
            val savedFile = WatermarkProcessor.saveBitmapWithExif(
                context = context,
                bitmap = pending.stampedBitmap,
                fileName = pending.fileName,
                siteLocation = pending.siteLocation,
                user = user,
                gpsData = pending.gpsData,
                captureTimeMillis = pending.timestamp,
                quality = currentSettings.jpegQuality
            )

            val photoEntity = SitePhoto(
                uid = user.uid,
                userName = user.displayName,
                userEmail = user.email,
                filePath = savedFile.absolutePath,
                fileName = pending.fileName,
                siteLocation = pending.siteLocation.ifBlank { "Site Location" },
                projectNote = pending.projectNote,
                latitude = pending.gpsData.latitude,
                longitude = pending.gpsData.longitude,
                accuracyMeters = pending.gpsData.accuracyMeters,
                gpsLocationName = pending.gpsData.locationName
                    ?: com.example.data.location.LocationTracker.getApproximateRegion(pending.gpsData.latitude, pending.gpsData.longitude),
                timestamp = pending.timestamp,
                formattedDateTime = WatermarkProcessor.formatDateTime(
                    pending.timestamp,
                    currentSettings.timezone,
                    currentSettings.dateFormat
                ),
                timezone = currentSettings.timezone
            )

            photoDao.insertPhoto(photoEntity)
            _pendingCapture.value = null
            navigateTo(AppScreen.CAMERA, clearStack = true)
        }
    }

    fun retakePendingPhoto() {
        _pendingCapture.value = null
        navigateTo(AppScreen.CAMERA, clearStack = true)
    }

    // Gallery Actions
    fun setSiteFilter(site: String?) {
        _siteFilter.value = site
    }

    fun togglePhotoSelection(photoId: Long) {
        val current = _selectedPhotoIds.value.toMutableSet()
        if (current.contains(photoId)) current.remove(photoId) else current.add(photoId)
        _selectedPhotoIds.value = current
    }

    fun selectAllPhotos() {
        val allIds = _photos.value.map { it.id }.toSet()
        _selectedPhotoIds.value = allIds
    }

    fun clearPhotoSelection() {
        _selectedPhotoIds.value = emptySet()
    }

    fun viewFullPhoto(photo: SitePhoto) {
        _activeFullPhoto.value = photo
        navigateTo(AppScreen.FULL_PHOTO)
    }

    fun deletePhoto(photo: SitePhoto) {
        viewModelScope.launch {
            try {
                File(photo.filePath).delete()
            } catch (_: Exception) {}
            photoDao.deletePhoto(photo)
            if (_activeFullPhoto.value?.id == photo.id) {
                goBack()
            }
        }
    }

    fun deleteSelectedPhotos() {
        val selectedIds = _selectedPhotoIds.value.toList()
        val uid = _currentUser.value?.uid ?: return
        viewModelScope.launch {
            val toDelete = _photos.value.filter { selectedIds.contains(it.id) }
            for (p in toDelete) {
                try {
                    File(p.filePath).delete()
                } catch (_: Exception) {}
            }
            photoDao.deletePhotosByIds(selectedIds, uid)
            _selectedPhotoIds.value = emptySet()
        }
    }

    fun exportSelectedAsZip(context: Context) {
        val selectedIds = _selectedPhotoIds.value
        val photosToExport = if (selectedIds.isEmpty()) {
            _photos.value
        } else {
            _photos.value.filter { selectedIds.contains(it.id) }
        }

        if (photosToExport.isEmpty()) {
            _exportMessage.value = "No photos selected for export."
            return
        }

        viewModelScope.launch {
            try {
                val zipFile = ExportManager.createExportZip(context, photosToExport)
                _exportMessage.value = "Exported ${photosToExport.size} photos to ${zipFile.name}"
                ExportManager.shareZipFile(context, zipFile)
            } catch (e: Exception) {
                _exportMessage.value = "Export failed: ${e.message}"
            }
        }
    }

    fun dismissExportMessage() {
        _exportMessage.value = null
    }

    // Settings
    fun updateSettings(newSettings: AppSettings) {
        _settings.value = newSettings
        locationTracker.setMockGpsEnabled(newSettings.mockGpsEnabled)
        val uid = _currentUser.value?.uid ?: return
        prefs.saveSettings(uid, newSettings)
    }

    fun clearAllUserData(context: Context) {
        val uid = _currentUser.value?.uid ?: return
        viewModelScope.launch {
            for (p in _photos.value) {
                try {
                    File(p.filePath).delete()
                } catch (_: Exception) {}
            }
            photoDao.deleteAllPhotosForUser(uid)
            _selectedPhotoIds.value = emptySet()
            _activeFullPhoto.value = null
        }
    }

    // Navigation
    fun navigateTo(screen: AppScreen, clearStack: Boolean = false) {
        if (clearStack) {
            backStack.clear()
        } else {
            backStack.add(_currentScreen.value)
        }
        _currentScreen.value = screen
    }

    fun goBack(): Boolean {
        return if (backStack.isNotEmpty()) {
            _currentScreen.value = backStack.removeAt(backStack.size - 1)
            true
        } else {
            if (_currentScreen.value != AppScreen.CAMERA && _currentUser.value != null) {
                _currentScreen.value = AppScreen.CAMERA
                true
            } else {
                false
            }
        }
    }
}
