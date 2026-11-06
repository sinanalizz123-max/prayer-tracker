package com.praytracker.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import com.praytracker.R
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility class for sharing prayer times as images via WhatsApp and other apps.
 */
object ShareUtils {
    
    /**
     * Share prayer times as an image via WhatsApp or other apps
     */
    fun sharePrayerTimesAsImage(
        context: Context,
        schedule: PrayerCalculator.PrayerSchedule,
        hijriDate: HijriHelper.HijriDateInfo
    ) {
        // Generate the bitmap
        val bitmap = createPrayerTimesBitmap(context, schedule, hijriDate)
        
        if (bitmap != null) {
            // Save bitmap to temporary file
            val imageUri = saveBitmapToFile(context, bitmap)
            
            if (imageUri != null) {
                // Create share intent
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, imageUri)
                    putExtra(Intent.EXTRA_SUBJECT, "Prayer Times - ${schedule.locationName}")
                    putExtra(Intent.EXTRA_TEXT, "Prayer times for ${schedule.formattedGregorianDate} (${hijriDate.formattedEn})\n\n${getPrayerTimesText(schedule)}")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                // Start chooser (allows user to select WhatsApp or other apps)
                val chooserIntent = Intent.createChooser(
                    shareIntent,
                    "Share Prayer Times"
                ).apply {
                    // Prefer WhatsApp if available
                    if (isPackageInstalled(context, "com.whatsapp")) {
                        setPackage("com.whatsapp")
                    }
                }
                
                context.startActivity(chooserIntent)
            }
        }
    }
    
    /**
     * Create a bitmap with prayer times information
     */
    private fun createPrayerTimesBitmap(
        context: Context,
        schedule: PrayerCalculator.PrayerSchedule,
        hijriDate: HijriHelper.HijriDateInfo
    ): Bitmap? {
        return try {
            // Calculate dimensions
            val width = 800
            val height = 1200
            
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            
            // Background
            canvas.drawColor(Color.WHITE)
            
            // Get colors
            val primaryColor = context.getColor(android.R.color.holo_blue_dark)
            val textColor = Color.BLACK
            val secondaryColor = context.getColor(android.R.color.holo_green_dark)
            
            // Create paints
            val titlePaint = Paint().apply {
                color = primaryColor
                textSize = 48f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            
            val subtitlePaint = Paint().apply {
                color = textColor
                textSize = 32f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            
            val prayerNamePaint = Paint().apply {
                color = textColor
                textSize = 36f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.LEFT
                isAntiAlias = true
            }
            
            val prayerTimePaint = Paint().apply {
                color = primaryColor
                textSize = 36f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
                isAntiAlias = true
            }
            
            val dividerPaint = Paint().apply {
                color = Color.LTGRAY
                strokeWidth = 2f
                isAntiAlias = true
            }
            
            val eventPaint = Paint().apply {
                color = secondaryColor
                textSize = 28f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            
            // Draw content
            var y = 80f
            
            // Title
            canvas.drawText("Prayer Times", width / 2f, y, titlePaint)
            y += 60f
            
            // Location
            canvas.drawText(schedule.locationName, width / 2f, y, subtitlePaint)
            y += 40f
            
            // Date
            canvas.drawText(schedule.formattedGregorianDate, width / 2f, y, subtitlePaint)
            y += 40f
            
            // Hijri Date
            canvas.drawText(hijriDate.formattedEn, width / 2f, y, subtitlePaint)
            y += 50f
            
            // Event if available
            hijriDate.eventName?.let { event ->
                canvas.drawText(event, width / 2f, y, eventPaint)
                y += 40f
            }
            
            y += 20f
            
            // Draw divider
            canvas.drawLine(50f, y, width - 50f, y, dividerPaint)
            y += 30f
            
            // Prayer times
            val prayerSpacing = 60f
            for (prayer in schedule.list) {
                if (prayer.isSunrise) continue // Skip sunrise
                
                canvas.drawText(prayer.name, 60f, y + 20f, prayerNamePaint)
                canvas.drawText(prayer.formattedTime, width - 60f, y + 20f, prayerTimePaint)
                
                // Draw divider between prayers
                canvas.drawLine(60f, y + 50f, width - 60f, y + 50f, dividerPaint)
                y += prayerSpacing
            }
            
            // Footer
            y += 40f
            val footerPaint = Paint().apply {
                color = Color.GRAY
                textSize = 24f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("Generated by Prayer Tracker", width / 2f, y, footerPaint)
            
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Save bitmap to a temporary file and return its URI
     */
    private fun saveBitmapToFile(context: Context, bitmap: Bitmap): Uri? {
        return try {
            // Create directory if it doesn't exist
            val directory = File(context.cacheDir, "shared_images")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            
            // Create file
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(directory, "prayer_times_$timestamp.png")
            
            // Save bitmap
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            
            // Return URI using FileProvider
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Get prayer times as formatted text
     */
    private fun getPrayerTimesText(schedule: PrayerCalculator.PrayerSchedule): String {
        return schedule.list
            .filter { !it.isSunrise }
            .joinToString("\n") { "${it.name}: ${it.formattedTime}" }
    }
    
    /**
     * Check if a package is installed
     */
    private fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }
}
