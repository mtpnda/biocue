package com.mtpnda.biocue.role

import com.mtpnda.biocue.data.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoleStoreTest {
    @Test fun `parse accepts only exact role names`() {
        assertEquals(UserRole.WEARER, RoleStore.parse("WEARER"))
        assertEquals(UserRole.CAREGIVER, RoleStore.parse("CAREGIVER"))
        assertNull(RoleStore.parse("wearer")); assertNull(RoleStore.parse("")); assertNull(RoleStore.parse(null))
    }

    @Test fun `only an explicit caregiver is refused watch data`() {
        assertFalse(RoleStore.mayReceiveWatchData(UserRole.CAREGIVER))
        assertTrue(RoleStore.mayReceiveWatchData(UserRole.WEARER))
        assertTrue("unknown role must not drop an upgrading wearer's data", RoleStore.mayReceiveWatchData(null))
    }
}
