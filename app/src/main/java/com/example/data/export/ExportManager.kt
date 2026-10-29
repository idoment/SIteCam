package com.example.data.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.model.SitePhoto
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ExportManager {

    /**
     * Builds a CSV string of photo metadata.
     */
    fun generateCsv(photos: List<SitePhoto>): String {
        val sb = StringBuilder()
        sb.append("File Name,User Name,User Email,Site Location,GPS Location Name,Project/Note,Latitude,Longitude,Accuracy (m),Date & Time,Timezone\n")
        for (p in photos) {
            val safeFileName = escapeCsv(p.fileName)
            val safeName = escapeCsv(p.userName)
            val safeEmail = escapeCsv(p.userEmail)
            val safeSite = escapeCsv(p.siteLocation)
            val safeLocName = escapeCsv(p.gpsLocationName ?: "")
            val safeNote = escapeCsv(p.projectNote)
            val lat = p.latitude?.toString() ?: ""
            val lng = p.longitude?.toString() ?: ""
            val acc = p.accuracyMeters?.toString() ?: ""
            val dt = escapeCsv(p.formattedDateTime)
            val tz = escapeCsv(p.timezone)
            sb.append("$safeFileName,$safeName,$safeEmail,$safeSite,$safeLocName,$safeNote,$lat,$lng,$acc,$dt,$tz\n")
        }
        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    /**
     * Creates a ZIP file containing the selected photos and the CSV log.
     */
    fun createExportZip(context: Context, photos: List<SitePhoto>): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val zipFile = File(exportDir, "MAXINFRA_SiteCam_Export_$timeStamp.zip")

        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
            // 1. Add CSV file
            val csvContent = generateCsv(photos)
            val csvEntry = ZipEntry("photos_log.csv")
            zos.putNextEntry(csvEntry)
            zos.write(csvContent.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 2. Add each photo file
            val buffer = ByteArray(8192)
            for (photo in photos) {
                val photoFile = File(photo.filePath)
                if (photoFile.exists()) {
                    val entry = ZipEntry("photos/${photo.fileName}")
                    zos.putNextEntry(entry)
                    BufferedInputStream(FileInputStream(photoFile)).use { bis ->
                        var count: Int
                        while (bis.read(buffer).also { count = it } != -1) {
                            zos.write(buffer, 0, count)
                        }
                    }
                    zos.closeEntry()
                }
            }
        }
        return zipFile
    }

    /**
     * Shares photos or ZIP via Android Intent.
     */
    fun shareZipFile(context: Context, zipFile: File) {
        val authority = "${context.packageName}.fileprovider"
        val contentUri: Uri = FileProvider.getUriForFile(context, authority, zipFile)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "MAXINFRA SiteCam Photo Export")
            putExtra(Intent.EXTRA_TEXT, "Exported ${zipFile.name} with site photos and evidence log.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share SiteCam ZIP Export"))
    }

    fun shareSinglePhoto(context: Context, photo: SitePhoto) {
        val photoFile = File(photo.filePath)
        if (!photoFile.exists()) return

        val authority = "${context.packageName}.fileprovider"
        val contentUri: Uri = FileProvider.getUriForFile(context, authority, photoFile)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "Site Photo: ${photo.siteLocation}")
            putExtra(
                Intent.EXTRA_TEXT,
                "Site: ${photo.siteLocation}\nCoordinates: ${photo.latitude}, ${photo.longitude}\nCaptured by: ${photo.userName} at ${photo.formattedDateTime}"
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Site Photo"))
    }

    fun shareMultiplePhotos(context: Context, photos: List<SitePhoto>) {
        val authority = "${context.packageName}.fileprovider"
        val uris = ArrayList<Uri>()
        for (photo in photos) {
            val file = File(photo.filePath)
            if (file.exists()) {
                uris.add(FileProvider.getUriForFile(context, authority, file))
            }
        }
        if (uris.isEmpty()) return

        val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/jpeg"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            putExtra(Intent.EXTRA_SUBJECT, "MAXINFRA SiteCam - ${photos.size} Photos")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share ${photos.size} Photos"))
    }
}
