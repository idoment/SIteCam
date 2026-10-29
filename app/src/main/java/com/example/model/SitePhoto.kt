package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "site_photos")
data class SitePhoto(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uid: String,
    val userName: String,
    val userEmail: String,
    val filePath: String,
    val fileName: String,
    val siteLocation: String,
    val projectNote: String,
    val latitude: Double?,
    val longitude: Double?,
    val accuracyMeters: Float?,
    val gpsLocationName: String? = null,
    val timestamp: Long,
    val formattedDateTime: String,
    val timezone: String
)

data class UserSession(
    val uid: String,
    val displayName: String,
    val email: String,
    val photoUrl: String? = null,
    val isTestMode: Boolean = false,
    val phoneNumber: String = ""
)

enum class GpsStatus {
    LOCKED,   // < 20 meters accuracy
    WEAK,     // >= 20 meters
    WAITING,  // No fix yet
    UNAVAILABLE
}

data class GpsData(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracyMeters: Float? = null,
    val status: GpsStatus = GpsStatus.WAITING,
    val lastUpdateTime: Long = 0L,
    val locationName: String? = null
)

data class AppSettings(
    val timezone: String = "Asia/Kolkata",
    val dateFormat: String = "dd-MM-yyyy HH:mm:ss",
    val watermarkPosition: String = "BOTTOM", // "BOTTOM" or "TOP"
    val textSize: String = "MEDIUM", // "SMALL", "MEDIUM", "LARGE"
    val watermarkOpacity: Float = 0.65f,
    val showCapturedBy: Boolean = true,
    val showBrandStamp: Boolean = true,
    val jpegQuality: Int = 85,
    val allowCaptureWithoutGps: Boolean = false,
    val mockGpsEnabled: Boolean = false
)
