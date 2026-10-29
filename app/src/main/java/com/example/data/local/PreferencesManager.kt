package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.config.AppConfig
import com.example.model.AppSettings
import com.example.model.UserSession
import org.json.JSONArray

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("sitecam_prefs", Context.MODE_PRIVATE)

    // User Session
    fun saveUserSession(session: UserSession) {
        prefs.edit().apply {
            putString("user_uid", session.uid)
            putString("user_name", session.displayName)
            putString("user_email", session.email)
            putString("user_photo", session.photoUrl)
            putBoolean("user_is_test", session.isTestMode)
            putString("user_phone", session.phoneNumber)
            apply()
        }
    }

    fun getUserSession(): UserSession? {
        val uid = prefs.getString("user_uid", null) ?: return null
        val name = prefs.getString("user_name", "Field Engineer") ?: "Field Engineer"
        val email = prefs.getString("user_email", "") ?: ""
        val photo = prefs.getString("user_photo", null)
        val isTest = prefs.getBoolean("user_is_test", false)
        val phone = prefs.getString("user_phone", "+91 98765 43210") ?: "+91 98765 43210"
        return UserSession(uid, name, email, photo, isTest, phone)
    }

    fun clearUserSession() {
        prefs.edit().apply {
            remove("user_uid")
            remove("user_name")
            remove("user_email")
            remove("user_photo")
            remove("user_is_test")
            remove("user_phone")
            apply()
        }
    }

    // Site Locations
    fun getLastSiteLocation(uid: String): String {
        return prefs.getString("last_site_$uid", "Plot 14, Solar Plant, Anantapur, AP") ?: "Plot 14, Solar Plant, Anantapur, AP"
    }

    fun saveLastSiteLocation(uid: String, location: String) {
        prefs.edit().putString("last_site_$uid", location).apply()
        addRecentLocation(uid, location)
    }

    fun getLastProjectNote(uid: String): String {
        return prefs.getString("last_note_$uid", "") ?: ""
    }

    fun saveLastProjectNote(uid: String, note: String) {
        prefs.edit().putString("last_note_$uid", note).apply()
    }

    fun getRecentLocations(uid: String): List<String> {
        val jsonStr = prefs.getString("recent_sites_$uid", null) ?: return listOf(
            "Plot 14, Solar Plant, Anantapur, AP",
            "Block B, Substation Substation-02",
            "Tower 44, Transmission Line Corridors",
            "Control Room Yard, Site Alpha"
        )
        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun addRecentLocation(uid: String, location: String) {
        if (location.isBlank()) return
        val current = getRecentLocations(uid).toMutableList()
        current.remove(location)
        current.add(0, location)
        val trimmed = if (current.size > 10) current.subList(0, 10) else current
        val jsonArray = JSONArray()
        trimmed.forEach { jsonArray.put(it) }
        prefs.edit().putString("recent_sites_$uid", jsonArray.toString()).apply()
    }

    // Settings per user
    fun getSettings(uid: String): AppSettings {
        return AppSettings(
            timezone = prefs.getString("setting_tz_$uid", AppConfig.DEFAULT_TIMEZONE) ?: AppConfig.DEFAULT_TIMEZONE,
            dateFormat = prefs.getString("setting_df_$uid", AppConfig.DEFAULT_DATE_FORMAT) ?: AppConfig.DEFAULT_DATE_FORMAT,
            watermarkPosition = prefs.getString("setting_pos_$uid", AppConfig.DEFAULT_WATERMARK_POSITION) ?: AppConfig.DEFAULT_WATERMARK_POSITION,
            textSize = prefs.getString("setting_size_$uid", AppConfig.DEFAULT_TEXT_SIZE) ?: AppConfig.DEFAULT_TEXT_SIZE,
            watermarkOpacity = prefs.getFloat("setting_opacity_$uid", AppConfig.DEFAULT_BG_OPACITY),
            showCapturedBy = prefs.getBoolean("setting_captured_by_$uid", AppConfig.DEFAULT_SHOW_CAPTURED_BY),
            showBrandStamp = prefs.getBoolean("setting_brand_stamp_$uid", AppConfig.DEFAULT_SHOW_BRAND_STAMP),
            jpegQuality = prefs.getInt("setting_quality_$uid", AppConfig.DEFAULT_JPEG_QUALITY),
            allowCaptureWithoutGps = prefs.getBoolean("setting_allow_no_gps_$uid", AppConfig.DEFAULT_ALLOW_CAPTURE_NO_GPS),
            mockGpsEnabled = prefs.getBoolean("setting_mock_gps_$uid", false)
        )
    }

    fun saveSettings(uid: String, settings: AppSettings) {
        prefs.edit().apply {
            putString("setting_tz_$uid", settings.timezone)
            putString("setting_df_$uid", settings.dateFormat)
            putString("setting_pos_$uid", settings.watermarkPosition)
            putString("setting_size_$uid", settings.textSize)
            putFloat("setting_opacity_$uid", settings.watermarkOpacity)
            putBoolean("setting_captured_by_$uid", settings.showCapturedBy)
            putBoolean("setting_brand_stamp_$uid", settings.showBrandStamp)
            putInt("setting_quality_$uid", settings.jpegQuality)
            putBoolean("setting_allow_no_gps_$uid", settings.allowCaptureWithoutGps)
            putBoolean("setting_mock_gps_$uid", settings.mockGpsEnabled)
            apply()
        }
    }
}
