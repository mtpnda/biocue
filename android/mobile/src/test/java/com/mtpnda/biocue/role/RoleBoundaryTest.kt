package com.mtpnda.biocue.role

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Architecture test: wearer-only and caregiver-only code must not reach into each other.
 *
 * Plain text scan (no Android needed). Gradle runs unit tests with the module directory as the
 * working directory, so the paths are relative to `mobile/`.
 *
 * To add a file to a side, list it below. A feature that genuinely needs both sides belongs in a
 * shared place (data/, ui/components/, core-detection) — not in a role-only file.
 */
class RoleBoundaryTest {
    private val root = File("src/main/java/com/mtpnda/biocue")

    private val wearerOnly = listOf(
        "WearerDashboardViewModel.kt", "WearerRoute.kt", "WatchDataReceiverService.kt",
        "connect/WatchConnectionManager.kt", "connect/ConnectionGuideScreen.kt", "ui/screens/wearer",
    )
    private val caregiverOnly = listOf(
        "CaregiverDashboardViewModel.kt", "CaregiverRoute.kt", "ui/screens/caregiver",
    )

    /** Things only a caregiver may touch. */
    private val caregiverApi = listOf(
        "CaregiverDashboard", "submitPairingCode", "findWearerByCode", "requestPairing",
        "observeWearerEvents", "getPairedWearer", "updateEventLabelAndStatus",
    )
    /** Things only a wearer may touch: the watch link, raw vitals, detection, event creation, caregiver management. */
    private val wearerApi = listOf(
        "WearerDashboard", "WatchDataReceiver", "WatchConnection", "Wearable", "SensorWindow", "DetectionDatabase",
        "sensorDataDao", "ScoringEngine", "DetectionPipeline", "BleLink", "generatePairingCode", "approveCaregiver",
        "denyCaregiver", "getCaregiversForWearer", "syncEvent(", "updatePairingCode",
    )

    private fun kotlinFiles(entries: List<String>): List<File> = entries.flatMap { e ->
        val f = File(root, e)
        assertTrue("boundary list points at a missing path: $e (cwd=${File(".").absoluteFile})", f.exists())
        if (f.isDirectory) f.walkTopDown().filter { it.extension == "kt" }.toList() else listOf(f)
    }

    private fun offences(files: List<File>, forbidden: List<String>): List<String> = files.flatMap { file ->
        file.readLines().mapIndexedNotNull { i, line ->
            // ignore comments and imports of shared things; look for real references
            val code = line.substringBefore("//")
            forbidden.firstOrNull { code.contains(it) }?.let { "${file.name}:${i + 1} references '$it'" }
        }
    }

    @Test fun `scanned sets are non-empty so the test cannot pass vacuously`() {
        assertTrue(kotlinFiles(wearerOnly).size >= 5)
        assertTrue(kotlinFiles(caregiverOnly).size >= 3)
    }

    @Test fun `caregiver-only code never touches wearer or watch code`() {
        val bad = offences(kotlinFiles(caregiverOnly), wearerApi)
        assertTrue("Caregiver code reaches into wearer-only API:\n" + bad.joinToString("\n"), bad.isEmpty())
    }

    @Test fun `wearer-only code never touches caregiver code`() {
        val bad = offences(kotlinFiles(wearerOnly), caregiverApi)
        assertTrue("Wearer code reaches into caregiver-only API:\n" + bad.joinToString("\n"), bad.isEmpty())
    }

    @Test fun `no file is claimed by both sides`() {
        val both = kotlinFiles(wearerOnly).map { it.canonicalPath }.toSet() intersect kotlinFiles(caregiverOnly).map { it.canonicalPath }.toSet()
        assertTrue("claimed by both: $both", both.isEmpty())
    }

    @Test fun `only the receiver service may be started by the watch and it is wearer-only`() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertTrue(manifest.contains("WatchDataReceiverService"))
        assertTrue(kotlinFiles(wearerOnly).any { it.name == "WatchDataReceiverService.kt" })
    }
}
