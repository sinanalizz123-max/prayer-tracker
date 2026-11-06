package com.praytracker.util

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import com.praytracker.R

/**
 * Manages Adhan audio playback for prayer notifications.
 * Supports both bundled audio files and custom URIs.
 */
class AdhanPlayer(private val context: Context) {
    
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    
    // Default Adhan audio files (bundled with the app)
    enum class AdhanType {
        FAJR,
        DHUHR,
        ASR,
        MAGHRIB,
        ISHA,
        GENERIC
    }
    
    /**
     * Play Adhan audio for a specific prayer
     */
    fun playAdhan(adhanType: AdhanType, volume: Float = 1.0f, loop: Boolean = false) {
        stopAdhan() // Stop any currently playing Adhan
        
        val resourceId = when (adhanType) {
            AdhanType.FAJR -> R.raw.adhan_fajr
            AdhanType.DHUHR -> R.raw.adhan_dhuhr
            AdhanType.ASR -> R.raw.adhan_asr
            AdhanType.MAGHRIB -> R.raw.adhan_maghrib
            AdhanType.ISHA -> R.raw.adhan_isha
            AdhanType.GENERIC -> R.raw.adhan_generic
        }
        
        playAdhan(resourceId, volume, loop)
    }
    
    /**
     * Play Adhan from a custom URI
     */
    fun playAdhanFromUri(uri: Uri, volume: Float = 1.0f, loop: Boolean = false) {
        stopAdhan()
        
        try {
            mediaPlayer = MediaPlayer.create(context, uri)
            mediaPlayer?.apply {
                isLooping = loop
                setVolume(volume, volume)
                setOnCompletionListener {
                    cleanup()
                }
                start()
                isPlaying = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            cleanup()
        }
    }
    
    /**
     * Play Adhan from a resource ID
     */
    private fun playAdhan(resourceId: Int, volume: Float, loop: Boolean) {
        try {
            mediaPlayer = MediaPlayer.create(context, resourceId)
            mediaPlayer?.apply {
                isLooping = loop
                setVolume(volume, volume)
                setOnCompletionListener {
                    cleanup()
                }
                start()
                isPlaying = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            cleanup()
        }
    }
    
    /**
     * Stop the currently playing Adhan
     */
    fun stopAdhan() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        cleanup()
    }
    
    /**
     * Pause the currently playing Adhan
     */
    fun pauseAdhan() {
        mediaPlayer?.apply {
            if (isPlaying) {
                pause()
                isPlaying = false
            }
        }
    }
    
    /**
     * Resume the paused Adhan
     */
    fun resumeAdhan() {
        mediaPlayer?.apply {
            if (!isPlaying) {
                start()
                isPlaying = true
            }
        }
    }
    
    /**
     * Check if Adhan is currently playing
     */
    fun isAdhanPlaying(): Boolean = isPlaying
    
    /**
     * Set volume (0.0 to 1.0)
     */
    fun setVolume(volume: Float) {
        mediaPlayer?.setVolume(volume, volume)
    }
    
    /**
     * Clean up media player resources
     */
    private fun cleanup() {
        mediaPlayer?.release()
        mediaPlayer = null
        isPlaying = false
    }
    
    /**
     * Release all resources
     */
    fun release() {
        stopAdhan()
        cleanup()
    }
}
