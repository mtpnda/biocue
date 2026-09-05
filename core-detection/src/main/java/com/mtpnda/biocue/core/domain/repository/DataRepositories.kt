package com.mtpnda.biocue.core.domain.repository

import com.mtpnda.biocue.core.domain.model.SensorWindow
import kotlinx.coroutines.flow.Flow

data class LabeledEpisode(
    val id: String,
    val isMeltdown: Boolean,
    val features: DoubleArray // Feature vector extracted from SensorWindow
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as LabeledEpisode
        if (id != other.id) return false
        if (isMeltdown != other.isMeltdown) return false
        if (!features.contentEquals(other.features)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + isMeltdown.hashCode()
        result = 31 * result + features.contentHashCode()
        return result
    }
}

interface SensorDataRepository {
    suspend fun insertWindow(window: SensorWindow)
    suspend fun getRecentWindows(limit: Int): List<SensorWindow>
    fun getLabeledEpisodes(): Flow<List<LabeledEpisode>>
}

interface MlModelRepository {
    suspend fun getLogisticRegressionWeights(): DoubleArray?
    suspend fun saveWeights(weights: DoubleArray)
}
