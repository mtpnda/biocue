package com.mtpnda.biocue.core.roles

import com.mtpnda.biocue.core.link.LinkRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class RoleCapabilitiesTest {
    @Test fun `wearer capabilities`() {
        val c = AccountRole.WEARER.capabilities()
        assertTrue(c.linkWatch); assertTrue(c.createEvents); assertTrue(c.manageCaregivers)
        assertFalse(c.requestPairing); assertFalse(c.readPairedWearerEvents); assertFalse(c.labelPairedWearerEvents)
    }

    @Test fun `caregiver capabilities`() {
        val c = AccountRole.CAREGIVER.capabilities()
        assertFalse(c.linkWatch); assertFalse(c.createEvents); assertFalse(c.manageCaregivers)
        assertTrue(c.requestPairing); assertTrue(c.readPairedWearerEvents); assertTrue(c.labelPairedWearerEvents)
    }

    @Test fun `the two roles never share a capability`() {
        val w = AccountRole.WEARER.capabilities(); val c = AccountRole.CAREGIVER.capabilities()
        assertTrue(w.linkWatch != c.linkWatch && w.createEvents != c.createEvents && w.manageCaregivers != c.manageCaregivers)
        assertTrue(w.requestPairing != c.requestPairing && w.readPairedWearerEvents != c.readPairedWearerEvents)
        assertTrue(w.labelPairedWearerEvents != c.labelPairedWearerEvents)
    }

    @Test fun `gate throws for the wrong role and passes for the right one`() {
        try { RoleGate(AccountRole.CAREGIVER).require("link watch") { it.linkWatch }; fail("should throw") }
        catch (e: RoleNotPermittedException) { assertEquals(AccountRole.CAREGIVER, e.role) }
        RoleGate(AccountRole.WEARER).require("link watch") { it.linkWatch }
        assertTrue(RoleGate(AccountRole.CAREGIVER).allows { it.labelPairedWearerEvents })
    }

    @Test fun `link role mapping matches the wire bytes`() {
        assertEquals(LinkRole.WEARER, AccountRole.WEARER.linkRole())
        assertEquals(LinkRole.CAREGIVER, AccountRole.CAREGIVER.linkRole())
        assertEquals(listOf("WEARER", "CAREGIVER"), AccountRole.values().map { it.name })
    }
}
