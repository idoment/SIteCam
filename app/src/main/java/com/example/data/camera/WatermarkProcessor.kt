package com.example.data.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.media.ExifInterface
import com.example.config.AppConfig
import com.example.model.AppSettings
import com.example.model.GpsData
import com.example.model.UserSession
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max

object WatermarkProcessor {

    fun formatCoordinates(lat: Double?, lng: Double?, accuracy: Float?): String {
        if (lat == null || lng == null) return "GPS unavailable"
        val latDir = if (lat >= 0) "N" else "S"
        val lngDir = if (lng >= 0) "E" else "W"
        val accStr = if (accuracy != null) " (±${String.format(Locale.US, "%.1f", accuracy)} m)" else ""
        return String.format(Locale.US, "%.6f° %s, %.6f° %s%s", kotlin.math.abs(lat), latDir, kotlin.math.abs(lng), lngDir, accStr)
    }

    fun formatDateTime(timestamp: Long, timezoneId: String, pattern: String): String {
        return try {
            val sdf = SimpleDateFormat(pattern, Locale.US)
            sdf.timeZone = TimeZone.getTimeZone(timezoneId)
            sdf.format(Date(timestamp)) + " ($timezoneId)"
        } catch (_: Exception) {
            val sdf = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone(AppConfig.DEFAULT_TIMEZONE)
            sdf.format(Date(timestamp)) + " (${AppConfig.DEFAULT_TIMEZONE})"
        }
    }

    /**
     * Burns the watermark permanently into the bitmap pixels.
     * Scales proportionally with the bitmap dimensions.
     */
    fun burnWatermark(
        sourceBitmap: Bitmap,
        siteLocation: String,
        projectNote: String,
        gpsData: GpsData,
        user: UserSession,
        settings: AppSettings,
        captureTimeMillis: Long
    ): Bitmap {
        // Create mutable copy
        val mutableBitmap = sourceBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val width = mutableBitmap.width.toFloat()
        val height = mutableBitmap.height.toFloat()

        // Base text size scale based on width (standardized to 1080px base)
        val scaleFactor = max(width, height) / 1080f
        val userScale = when (settings.textSize) {
            "SMALL" -> 0.85f
            "LARGE" -> 1.2f
            else -> 1.0f
        }

        val titleTextSize = 28f * scaleFactor * userScale
        val bodyTextSize = 20f * scaleFactor * userScale
        val smallTextSize = 16f * scaleFactor * userScale
        val lineSpacing = 6f * scaleFactor
        val horizontalPadding = 24f * scaleFactor
        val verticalPadding = 20f * scaleFactor

        // Prepare text lines
        val lines = mutableListOf<WatermarkLine>()

        // Line 1: Site Location (Bold, Largest, Amber Accent badge or text)
        val displaySite = if (siteLocation.isBlank()) "Site: Unspecified Location" else siteLocation.trim()
        lines.add(WatermarkLine("SITE: $displaySite", titleTextSize, Typeface.BOLD, Color.WHITE, isTitle = true))

        // Line 2: GPS Coordinates
        val coordText = formatCoordinates(gpsData.latitude, gpsData.longitude, gpsData.accuracyMeters)
        lines.add(WatermarkLine("GPS: $coordText", bodyTextSize, Typeface.NORMAL, Color.parseColor("#E2E8F0")))

        // Line 3: GPS Location Name (Resolved City/Region/State)
        val locName = gpsData.locationName?.trim()
            ?: if (gpsData.latitude != null && gpsData.longitude != null) {
                com.example.data.location.LocationTracker.getApproximateRegion(gpsData.latitude, gpsData.longitude)
            } else null
        if (!locName.isNullOrBlank()) {
            lines.add(WatermarkLine("GPS LOCATION: $locName", bodyTextSize, Typeface.NORMAL, Color.parseColor("#FEF08A")))
        }

        // Line 4: Timestamp
        val dateText = formatDateTime(captureTimeMillis, settings.timezone, settings.dateFormat)
        lines.add(WatermarkLine("TIME: $dateText", bodyTextSize, Typeface.NORMAL, Color.parseColor("#E2E8F0")))

        // Line 5: Captured by (if enabled)
        if (settings.showCapturedBy) {
            val phoneSuffix = if (user.phoneNumber.isNotBlank()) " (${user.phoneNumber})" else ""
            lines.add(WatermarkLine("Captured by: ${user.displayName}$phoneSuffix", bodyTextSize, Typeface.NORMAL, Color.parseColor("#CBD5E1")))
        }

        // Line 6: Project note (if not blank)
        if (projectNote.isNotBlank()) {
            lines.add(WatermarkLine("Project / Note: ${projectNote.trim()}", bodyTextSize, Typeface.ITALIC, Color.parseColor("#FDE68A")))
        }

        // Calculate total content height
        var totalTextHeight = 0f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        for (line in lines) {
            paint.textSize = line.size
            paint.typeface = Typeface.create(Typeface.DEFAULT, line.style)
            val bounds = Rect()
            paint.getTextBounds(line.text, 0, line.text.length, bounds)
            totalTextHeight += line.size + lineSpacing
        }

        val bannerHeight = totalTextHeight + (verticalPadding * 2)

        // Determine banner Y coordinates
        val isTop = settings.watermarkPosition.equals("TOP", ignoreCase = true)
        val bannerTop = if (isTop) 0f else (height - bannerHeight)
        val bannerBottom = if (isTop) bannerHeight else height

        // Draw semi-transparent dark navy background banner
        val bgPaint = Paint().apply {
            color = Color.parseColor("#0B1F3A")
            alpha = (settings.watermarkOpacity.coerceIn(0.1f, 1.0f) * 255).toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, bannerTop, width, bannerBottom, bgPaint)

        // Draw amber decorative accent bar (4dp equivalent)
        val accentHeight = 5f * scaleFactor
        val accentPaint = Paint().apply {
            color = Color.parseColor("#F59E0B")
            style = Paint.Style.FILL
        }
        val accentY = if (isTop) bannerBottom - accentHeight else bannerTop
        canvas.drawRect(0f, accentY, width, accentY + accentHeight, accentPaint)

        // Draw lines
        var currentY = bannerTop + verticalPadding + (if (isTop) 0f else accentHeight)

        for (line in lines) {
            paint.textSize = line.size
            paint.color = line.color
            paint.typeface = Typeface.create(Typeface.DEFAULT, line.style)
            // Add subtle shadow for maximum legibility on any background
            paint.setShadowLayer(4f * scaleFactor, 1f, 2f, Color.argb(200, 0, 0, 0))

            currentY += line.size
            canvas.drawText(line.text, horizontalPadding, currentY, paint)
            currentY += lineSpacing
        }

        return mutableBitmap
    }

    /**
     * Saves bitmap as JPEG and writes EXIF metadata.
     */
    fun saveBitmapWithExif(
        context: Context,
        bitmap: Bitmap,
        fileName: String,
        siteLocation: String,
        user: UserSession,
        gpsData: GpsData,
        captureTimeMillis: Long,
        quality: Int
    ): File {
        val picturesDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
            ?: context.filesDir
        val file = File(picturesDir, fileName)

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(50, 100), out)
            out.flush()
        }

        // Write EXIF Tags
        try {
            val exif = ExifInterface(file.absolutePath)
            val exifDateFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
            exif.setAttribute(ExifInterface.TAG_DATETIME, exifDateFormat.format(Date(captureTimeMillis)))
            exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, exifDateFormat.format(Date(captureTimeMillis)))
            exif.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, exifDateFormat.format(Date(captureTimeMillis)))
            exif.setAttribute(ExifInterface.TAG_MAKE, "MAXINFRA SiteCam")
            exif.setAttribute(ExifInterface.TAG_MODEL, "SiteCam Stamp")
            exif.setAttribute(
                ExifInterface.TAG_USER_COMMENT,
                "Site: $siteLocation | User: ${user.displayName} (${user.email})"
            )

            if (gpsData.latitude != null && gpsData.longitude != null) {
                val lat = gpsData.latitude
                val lng = gpsData.longitude
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, decimalToDMS(kotlin.math.abs(lat)))
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, if (lat >= 0) "N" else "S")
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, decimalToDMS(kotlin.math.abs(lng)))
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, if (lng >= 0) "E" else "W")
            }

            exif.saveAttributes()
        } catch (_: Exception) {}

        return file
    }

    private fun decimalToDMS(coord: Double): String {
        val deg = coord.toInt()
        val min = ((coord - deg) * 60).toInt()
        val sec = (((coord - deg) * 60 - min) * 60 * 1000).toInt()
        return "$deg/1,$min/1,$sec/1000"
    }

    private data class WatermarkLine(
        val text: String,
        val size: Float,
        val style: Int,
        val color: Int,
        val isTitle: Boolean = false
    )
}
