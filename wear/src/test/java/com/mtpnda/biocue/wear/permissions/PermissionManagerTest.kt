package com.mtpnda.biocue.wear.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class PermissionManagerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        mockkStatic(ContextCompat::class)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun testFactoryReturnsCorrectInstance() {
        assertTrue(PermissionFactory.create(true) is DevPermissionManager)
        assertTrue(PermissionFactory.create(false) is ProdPermissionManager)
    }

    @Test
    fun testDevPermissionManagerBypassesChecks() {
        val manager = DevPermissionManager()
        
        // Even if all permissions are denied...
        every { ContextCompat.checkSelfPermission(any(), any()) } returns PackageManager.PERMISSION_DENIED
        
        // Dev manager should STILL return true to prevent getting stuck
        assertTrue(manager.hasAllPermissions(context))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun testProdPermissionManagerRequiresAllPermissions_BelowTiramisu() {
        val manager = ProdPermissionManager()
        
        // Case 1: All denied
        every { ContextCompat.checkSelfPermission(any(), any()) } returns PackageManager.PERMISSION_DENIED
        assertFalse(manager.hasAllPermissions(context))
        
        // Case 2: Only body sensors granted
        every { ContextCompat.checkSelfPermission(any(), Manifest.permission.BODY_SENSORS) } returns PackageManager.PERMISSION_GRANTED
        assertFalse(manager.hasAllPermissions(context))
        
        // Case 3: Both Body and Activity granted (Notifications not needed below Tiramisu)
        every { ContextCompat.checkSelfPermission(any(), Manifest.permission.ACTIVITY_RECOGNITION) } returns PackageManager.PERMISSION_GRANTED
        assertTrue(manager.hasAllPermissions(context))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun testProdPermissionManagerRequiresAllPermissions_TiramisuAndAbove() {
        val manager = ProdPermissionManager()
        
        // Case 1: Body and Activity granted, but Notifications denied
        every { ContextCompat.checkSelfPermission(any(), Manifest.permission.BODY_SENSORS) } returns PackageManager.PERMISSION_GRANTED
        every { ContextCompat.checkSelfPermission(any(), Manifest.permission.ACTIVITY_RECOGNITION) } returns PackageManager.PERMISSION_GRANTED
        every { ContextCompat.checkSelfPermission(any(), Manifest.permission.POST_NOTIFICATIONS) } returns PackageManager.PERMISSION_DENIED
        
        assertFalse(manager.hasAllPermissions(context))
        
        // Case 2: All 3 granted
        every { ContextCompat.checkSelfPermission(any(), Manifest.permission.POST_NOTIFICATIONS) } returns PackageManager.PERMISSION_GRANTED
        assertTrue(manager.hasAllPermissions(context))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun testRequiredPermissionsList_BelowTiramisu() {
        val devManager = DevPermissionManager()
        val prodManager = ProdPermissionManager()
        
        assertEquals(2, devManager.getRequiredPermissions().size)
        assertEquals(2, prodManager.getRequiredPermissions().size)
        assertFalse(devManager.getRequiredPermissions().contains(Manifest.permission.POST_NOTIFICATIONS))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun testRequiredPermissionsList_TiramisuAndAbove() {
        val devManager = DevPermissionManager()
        val prodManager = ProdPermissionManager()
        
        assertEquals(3, devManager.getRequiredPermissions().size)
        assertEquals(3, prodManager.getRequiredPermissions().size)
        assertTrue(devManager.getRequiredPermissions().contains(Manifest.permission.POST_NOTIFICATIONS))
    }
}
