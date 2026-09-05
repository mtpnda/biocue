package com.mtpnda.biocue.core

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mtpnda.biocue.core.data.local.DetectionDatabase
import com.mtpnda.biocue.core.data.local.EventDao
import com.mtpnda.biocue.core.data.local.SensorDataDao
import com.mtpnda.biocue.core.data.local.SensorWindowEntity
import com.mtpnda.biocue.core.data.local.StressEventEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class RoomDatabaseTest {

    private lateinit var db: DetectionDatabase
    private lateinit var sensorDao: SensorDataDao
    private lateinit var eventDao: EventDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, DetectionDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        sensorDao = db.sensorDataDao()
        eventDao = db.eventDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun sensorDataDao_insertsAndRetrievesRecentWindows() = runBlocking {
        val now = System.currentTimeMillis()
        val entity1 = SensorWindowEntity(
            timestamp = now - 2000,
            hrMean = 75.0,
            rmssd = 40.0,
            accMag = 0.1,
            repetitiveness = 0.2,
            hrSlope = 0.5,
            isOffBody = false,
            isExercising = false
        )
        val entity2 = SensorWindowEntity(
            timestamp = now,
            hrMean = 85.0,
            rmssd = 30.0,
            accMag = 0.3,
            repetitiveness = 0.6,
            hrSlope = 1.5,
            isOffBody = false,
            isExercising = false
        )

        sensorDao.insert(entity1)
        sensorDao.insert(entity2)

        val recent = sensorDao.getRecentWindows(1)
        assertEquals(1, recent.size)
        assertEquals(85.0, recent.first().hrMean, 0.01)
        assertEquals(now, recent.first().timestamp)
    }

    @Test
    fun eventDao_insertsUpdatesAndQueriesEvents() = runBlocking {
        val now = System.currentTimeMillis()
        val event = StressEventEntity(
            id = "evt_001",
            startTime = now,
            endTime = null,
            triggerType = "MANUAL",
            label = "NONE",
            isResolved = false
        )

        eventDao.insertEvent(event)

        val unresolved = eventDao.getUnresolvedEvents()
        assertEquals(1, unresolved.size)
        assertEquals("evt_001", unresolved.first().id)

        val latestBeforeUpdate = eventDao.getLatestEvent()
        assertNotNull(latestBeforeUpdate)
        assertEquals("evt_001", latestBeforeUpdate?.id)

        val updatedEvent = event.copy(label = "OVERWHELMED", isResolved = true, endTime = now + 5000)
        eventDao.updateEvent(updatedEvent)

        val unresolvedAfter = eventDao.getUnresolvedEvents()
        assertTrue(unresolvedAfter.isEmpty())

        val eventsToday = eventDao.getEventsSince(now - 1000)
        assertEquals(1, eventsToday.size)
        assertEquals("OVERWHELMED", eventsToday.first().label)
    }
}
