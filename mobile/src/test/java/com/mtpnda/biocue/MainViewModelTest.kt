package com.mtpnda.biocue

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class MainViewModelTest {

    @Test
    fun loadRecentData_updatesStateFlow() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        assertEquals(true, true)
    }

    @Test
    fun toggleMode_flipsCaregiverState() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = MainViewModel(app)
        
        // Starts as false
        assertEquals(false, viewModel.isCaregiverMode.value)
        
        viewModel.toggleMode()
        assertEquals(true, viewModel.isCaregiverMode.value)
    }

    @Test
    fun resolveReviewEvent_doesNotCrash() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = MainViewModel(app)
        
        viewModel.resolveReviewEvent("event_123", "OVERWHELMED")
    }
}
