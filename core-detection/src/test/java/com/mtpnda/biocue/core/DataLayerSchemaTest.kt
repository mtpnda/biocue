package com.mtpnda.biocue.core

import com.mtpnda.biocue.core.domain.model.DataLayerSchema
import org.junit.Assert.assertEquals
import org.junit.Test

class DataLayerSchemaTest {

    @Test
    fun schemaPaths_matchContracts() {
        assertEquals("/sensor_window", DataLayerSchema.PATH_SENSOR_WINDOW)
        assertEquals("/event_start", DataLayerSchema.PATH_EVENT_START)
        assertEquals("/event_label", DataLayerSchema.PATH_EVENT_LABEL)
        assertEquals("/event_end", DataLayerSchema.PATH_EVENT_END)
    }

    @Test
    fun schemaKeys_matchContracts() {
        assertEquals("timestamp", DataLayerSchema.KEY_TIMESTAMP)
        assertEquals("hrMean", DataLayerSchema.KEY_HR_MEAN)
        assertEquals("rmssd", DataLayerSchema.KEY_RMSSD)
        assertEquals("accMag", DataLayerSchema.KEY_ACC_MAG)
        assertEquals("repetitiveness", DataLayerSchema.KEY_REPETITIVENESS)
        assertEquals("hrSlope", DataLayerSchema.KEY_HR_SLOPE)
        assertEquals("isOffBody", DataLayerSchema.KEY_IS_OFF_BODY)
        assertEquals("isExercising", DataLayerSchema.KEY_IS_EXERCISING)
        assertEquals("eventId", DataLayerSchema.KEY_EVENT_ID)
        assertEquals("label", DataLayerSchema.KEY_EVENT_LABEL)
        assertEquals("triggerType", DataLayerSchema.KEY_EVENT_TRIGGER_TYPE)
    }
}
