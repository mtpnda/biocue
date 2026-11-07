package com.mtpnda.biocue.wear.profile

import android.content.Context
import android.util.AtomicFile
import android.util.Log
import com.mtpnda.biocue.core.ml.HrvBaselineProfiler
import com.mtpnda.biocue.core.ml.WearerProfileData
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets

/**
 * Battery-conscious atomic file persistence for the wearer's HRV and baseline profile.
 * Thread-safe, cached in memory, and writes to disk only during scheduled intervals or shutdown.
 */
class HrvProfileStore(private val context: Context) {
    private val file = File(context.filesDir, "hrv_profile.json")
    private val atomicFile = AtomicFile(file)

    @Volatile
    private var cachedProfiler: HrvBaselineProfiler? = null
    private var lastPersistedMs: Long = 0L

    companion object {
        private const val TAG = "HrvProfileStore"
        private const val MIN_PERSIST_INTERVAL_MS = 5 * 60 * 1000L // Write to flash at most once every 5 mins
    }

    @Synchronized
    fun getProfiler(): HrvBaselineProfiler {
        cachedProfiler?.let { return it }

        val loaded = loadFromDisk()
        val profiler = loaded ?: HrvBaselineProfiler()
        cachedProfiler = profiler
        return profiler
    }

    private fun loadFromDisk(): HrvBaselineProfiler? {
        if (!file.exists()) return null
        return try {
            val bytes = atomicFile.readFully()
            val jsonStr = String(bytes, StandardCharsets.UTF_8)
            val profiler = HrvBaselineProfiler.deserializeFromJson(jsonStr)
            Log.d(TAG, "Loaded HRV profile: calibrated samples = ${profiler?.profile?.calibratedSamples}")
            profiler
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read HRV profile from disk, fallback to default", e)
            null
        }
    }

    @Synchronized
    fun persistThrottled(profiler: HrvBaselineProfiler, force: Boolean = false) {
        cachedProfiler = profiler
        val now = System.currentTimeMillis()
        if (force || (now - lastPersistedMs >= MIN_PERSIST_INTERVAL_MS)) {
            persistToDisk(profiler)
            lastPersistedMs = now
        }
    }

    private fun persistToDisk(profiler: HrvBaselineProfiler) {
        var fos: FileOutputStream? = null
        try {
            val jsonStr = profiler.serializeToJson()
            val bytes = jsonStr.toByteArray(StandardCharsets.UTF_8)
            fos = atomicFile.startWrite()
            fos.write(bytes)
            atomicFile.finishWrite(fos)
            Log.d(TAG, "Successfully persisted HRV profile (size = ${bytes.size} bytes)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist HRV profile", e)
            if (fos != null) {
                atomicFile.failWrite(fos)
            }
        }
    }
}
