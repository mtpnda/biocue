package com.mtpnda.biocue

import com.mtpnda.biocue.wear.presentation.WatchState
import org.junit.Assert.assertEquals
import org.junit.Test

class WatchStateTest {

    private fun computeInitialWatchState(
        hasPermissions: Boolean,
        isPhoneReachable: Boolean,
        launchState: WatchState = WatchState.ANCHOR
    ): WatchState {
        return when {
            !hasPermissions -> WatchState.SETUP_PERMISSIONS
            !isPhoneReachable -> WatchState.SETUP_PHONE
            else -> launchState
        }
    }

    @Test
    fun initialWatchState_withoutPermissions_returnsSetupPermissions() {
        val state = computeInitialWatchState(
            hasPermissions = false,
            isPhoneReachable = true
        )
        assertEquals(WatchState.SETUP_PERMISSIONS, state)
    }

    @Test
    fun initialWatchState_withPermissions_phoneUnreachable_returnsSetupPhone() {
        val state = computeInitialWatchState(
            hasPermissions = true,
            isPhoneReachable = false
        )
        assertEquals(WatchState.SETUP_PHONE, state)
    }

    @Test
    fun initialWatchState_withPermissions_phoneReachable_returnsLaunchState() {
        val state = computeInitialWatchState(
            hasPermissions = true,
            isPhoneReachable = true,
            launchState = WatchState.ANCHOR
        )
        assertEquals(WatchState.ANCHOR, state)
    }

    @Test
    fun initialWatchState_withPermissions_phoneReachable_returnsCarousel_whenRequested() {
        val state = computeInitialWatchState(
            hasPermissions = true,
            isPhoneReachable = true,
            launchState = WatchState.CAROUSEL
        )
        assertEquals(WatchState.CAROUSEL, state)
    }
}
