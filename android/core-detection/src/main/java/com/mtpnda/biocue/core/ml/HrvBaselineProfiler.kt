package com.mtpnda.biocue.core.ml

import com.mtpnda.biocue.core.domain.model.Baseline
import com.mtpnda.biocue.core.domain.model.SensorWindow
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

/**
 * Encapsulates the wearer's personalized baseline profile, current vitals,
 * and compact hourly metrics rollup.
 */
data class WearerProfileData(
    val version: Int = 1,
    val updatedAtMs: Long = System.currentTimeMillis(),
    val calibratedSamples: Int = 0,
    val baseline: Baseline = Baseline(hourOfDay = 12, hrMean = 72.0, hrStd = 8.0, hrvMean = 42.0, hrvStd = 10.0),
    val latestHr: Double = 72.0,
    val latestRmssd: Double = 42.0,
    val latestStressPercent: Int = 0,
    val latestTier: Int = 0,
    val hourlyRollup: List<HourlyMetrics> = emptyList()
)

data class HourlyMetrics(
    val hourOfDay: Int,
    val avgHr: Double,
    val avgRmssd: Double,
    val avgStressPercent: Int
)

class HrvBaselineProfiler(
    initialBaseline: Baseline? = null,
    initialProfile: WearerProfileData? = null
) {
    var profile: WearerProfileData = initialProfile ?: WearerProfileData(
        baseline = initialBaseline ?: Baseline(hourOfDay = 12, hrMean = 72.0, hrStd = 8.0, hrvMean = 42.0, hrvStd = 10.0)
    )
        private set

    private val scoringEngine = ScoringEngine()

    /**
     * Compute normalized stress percentage (0-100%).
     */
    fun computeStressPercent(window: SensorWindow): Int {
        if (window.isOffBody) return 0
        val score = scoringEngine.computeRuleBasedScore(window, profile.baseline)
        // Standard sigmoid mapping centered around score = 1.0 (calm threshold)
        val z = 1.2 * (score - 1.0)
        val sig = 1.0 / (1.0 + exp(-z))
        val percent = (sig * 100.0).toInt()
        return min(100, max(0, percent))
    }

    /**
     * Incorporate a new sensor window. If the wearer is calm and resting,
     * adapt the baseline. Updates latest metrics and returns whether baseline adapted.
     */
    fun incorporateWindow(window: SensorWindow): Boolean {
        val stressPct = computeStressPercent(window)
        val tier = when {
            stressPct >= 75 -> 2
            stressPct >= 50 -> 1
            else -> 0
        }

        val canCalibrate = !window.isOffBody &&
                !window.isExercising &&
                window.accMag < 0.25 &&
                tier == 0 &&
                window.rmssd >= 15.0 && window.rmssd <= 150.0 &&
                window.hrMean >= 45.0 && window.hrMean <= 120.0

        val newBaseline = if (canCalibrate) {
            val n = profile.calibratedSamples + 1
            val alpha = 1.0 / min(n.toDouble(), 100.0)
            val updatedHrMean = (1.0 - alpha) * profile.baseline.hrMean + alpha * window.hrMean
            val updatedHrvMean = (1.0 - alpha) * profile.baseline.hrvMean + alpha * window.rmssd
            
            // Rolling standard deviation estimation
            val hrDelta = window.hrMean - updatedHrMean
            val updatedHrStd = max(2.0, (1.0 - alpha) * profile.baseline.hrStd + alpha * kotlin.math.abs(hrDelta))
            val hrvDelta = window.rmssd - updatedHrvMean
            val updatedHrvStd = max(2.0, (1.0 - alpha) * profile.baseline.hrvStd + alpha * kotlin.math.abs(hrvDelta))

            Baseline(
                hourOfDay = profile.baseline.hourOfDay,
                hrMean = updatedHrMean,
                hrStd = updatedHrStd,
                hrvMean = updatedHrvMean,
                hrvStd = updatedHrvStd
            )
        } else {
            profile.baseline
        }

        profile = profile.copy(
            updatedAtMs = window.timestamp,
            calibratedSamples = if (canCalibrate) profile.calibratedSamples + 1 else profile.calibratedSamples,
            baseline = newBaseline,
            latestHr = window.hrMean,
            latestRmssd = window.rmssd,
            latestStressPercent = stressPct,
            latestTier = tier
        )

        return canCalibrate
    }

    /**
     * Record hourly average vitals to rolling history.
     */
    fun recordHourlyRollup(hour: Int, avgHr: Double, avgRmssd: Double, avgStress: Int) {
        val entry = HourlyMetrics(hour, avgHr, avgRmssd, avgStress)
        val updated = (profile.hourlyRollup.filter { it.hourOfDay != hour } + entry).sortedBy { it.hourOfDay }
        profile = profile.copy(hourlyRollup = updated)
    }

    fun serializeToJson(): String {
        val json = JSONObject()
        json.put("version", profile.version)
        json.put("updatedAtMs", profile.updatedAtMs)
        json.put("calibratedSamples", profile.calibratedSamples)

        val bJson = JSONObject()
        bJson.put("hourOfDay", profile.baseline.hourOfDay)
        bJson.put("hrMean", profile.baseline.hrMean)
        bJson.put("hrStd", profile.baseline.hrStd)
        bJson.put("hrvMean", profile.baseline.hrvMean)
        bJson.put("hrvStd", profile.baseline.hrvStd)
        json.put("baseline", bJson)

        val mJson = JSONObject()
        mJson.put("latestHr", profile.latestHr)
        mJson.put("latestRmssd", profile.latestRmssd)
        mJson.put("latestStressPercent", profile.latestStressPercent)
        mJson.put("latestTier", profile.latestTier)
        json.put("currentMetrics", mJson)

        val rollupArray = JSONArray()
        for (h in profile.hourlyRollup) {
            val hObj = JSONObject()
            hObj.put("hourOfDay", h.hourOfDay)
            hObj.put("avgHr", h.avgHr)
            hObj.put("avgRmssd", h.avgRmssd)
            hObj.put("avgStressPercent", h.avgStressPercent)
            rollupArray.put(hObj)
        }
        json.put("hourlyRollup", rollupArray)

        return json.toString()
    }

    companion object {
        fun deserializeFromJson(jsonStr: String): HrvBaselineProfiler? {
            return try {
                val json = JSONObject(jsonStr)
                val ver = json.optInt("version", 1)
                val updated = json.optLong("updatedAtMs", System.currentTimeMillis())
                val samples = json.optInt("calibratedSamples", 0)

                val bObj = json.optJSONObject("baseline") ?: JSONObject()
                val baseline = Baseline(
                    hourOfDay = bObj.optInt("hourOfDay", 12),
                    hrMean = bObj.optDouble("hrMean", 72.0),
                    hrStd = bObj.optDouble("hrStd", 8.0),
                    hrvMean = bObj.optDouble("hrvMean", 42.0),
                    hrvStd = bObj.optDouble("hrvStd", 10.0)
                )

                val mObj = json.optJSONObject("currentMetrics") ?: JSONObject()
                val latestHr = mObj.optDouble("latestHr", 72.0)
                val latestRmssd = mObj.optDouble("latestRmssd", 42.0)
                val latestStress = mObj.optInt("latestStressPercent", 0)
                val latestTier = mObj.optInt("latestTier", 0)

                val rollupList = mutableListOf<HourlyMetrics>()
                val rArr = json.optJSONArray("hourlyRollup")
                if (rArr != null) {
                    for (i in 0 until rArr.length()) {
                        val item = rArr.getJSONObject(i)
                        rollupList.add(
                            HourlyMetrics(
                                hourOfDay = item.getInt("hourOfDay"),
                                avgHr = item.getDouble("avgHr"),
                                avgRmssd = item.getDouble("avgRmssd"),
                                avgStressPercent = item.getInt("avgStressPercent")
                            )
                        )
                    }
                }

                val profileData = WearerProfileData(
                    version = ver,
                    updatedAtMs = updated,
                    calibratedSamples = samples,
                    baseline = baseline,
                    latestHr = latestHr,
                    latestRmssd = latestRmssd,
                    latestStressPercent = latestStress,
                    latestTier = latestTier,
                    hourlyRollup = rollupList
                )
                HrvBaselineProfiler(initialProfile = profileData)
            } catch (e: Exception) {
                null
            }
        }
    }
}
