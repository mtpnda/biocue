package com.mtpnda.biocue.core.link

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvertisePolicyTest {
    @Test fun `the service id is advertised while the pairing window is open`() {
        assertTrue(AdvertisePolicy.includeServiceUuid(pairingWindowOpen = true))
    }


    @Test fun `once pairing is closed the advertisement carries no service id`() {
        assertFalse(AdvertisePolicy.includeServiceUuid(pairingWindowOpen = false))
    }
}
