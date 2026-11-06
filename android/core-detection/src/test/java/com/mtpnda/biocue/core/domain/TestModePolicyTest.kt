package com.mtpnda.biocue.core.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TestModePolicyTest {
    @Test fun `a release build never runs test mode even if it was requested`() {
        assertFalse(TestModePolicy.effective(isDebugBuild = false, requested = true))
    }


    @Test fun `a debug build runs test mode only when it is explicitly requested`() {
        assertTrue(TestModePolicy.effective(isDebugBuild = true, requested = true))
        assertFalse("off by default", TestModePolicy.effective(isDebugBuild = true, requested = false))
    }
}
