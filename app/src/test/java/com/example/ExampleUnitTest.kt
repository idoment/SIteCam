package com.example

import com.example.config.AppConfig
import com.example.data.camera.WatermarkProcessor
import com.example.data.export.ExportManager
import com.example.model.SitePhoto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testConfigConstants() {
        assertEquals("MAXINFRA SiteCam", AppConfig.APP_NAME)
        assertEquals(3000L, AppConfig.SPLASH_DURATION_MS)
        assertEquals("/assets/logo.png", AppConfig.LOGO_URL)
        assertEquals("Powered by APRICO", AppConfig.POWERED_BY_TEXT)
        assertEquals("Asia/Kolkata", AppConfig.DEFAULT_TIMEZONE)
    }

    @Test
    fun testCoordinateFormatting() {
        val formatted = WatermarkProcessor.formatCoordinates(14.681885, 77.600592, 4.2f)
        assertTrue(formatted.contains("14.681885° N"))
        assertTrue(formatted.contains("77.600592° E"))
        assertTrue(formatted.contains("±4.2 m"))

        val unavailable = WatermarkProcessor.formatCoordinates(null, null, null)
        assertEquals("GPS unavailable", unavailable)
    }

    @Test
    fun testCsvGeneration() {
        val photos = listOf(
            SitePhoto(
                id = 1L,
                uid = "test_001",
                userName = "Er. Arun Kumar",
                userEmail = "arun@maxinfra.in",
                filePath = "/path/to/photo.jpg",
                fileName = "Site_20261028_120000.jpg",
                siteLocation = "Plot 14, Solar Plant",
                projectNote = "Inverter check",
                latitude = 14.681885,
                longitude = 77.600592,
                accuracyMeters = 4.0f,
                gpsLocationName = "Anantapur, Andhra Pradesh, India",
                timestamp = 1761696000000L,
                formattedDateTime = "28-10-2026 12:00:00 (Asia/Kolkata)",
                timezone = "Asia/Kolkata"
            )
        )
        val csv = ExportManager.generateCsv(photos)
        assertTrue(csv.startsWith("File Name,User Name,User Email,Site Location,GPS Location Name"))
        assertTrue(csv.contains("Site_20261028_120000.jpg"))
        assertTrue(csv.contains("Plot 14, Solar Plant"))
        assertTrue(csv.contains("Anantapur, Andhra Pradesh, India"))
        assertTrue(csv.contains("14.681885"))
    }
}
