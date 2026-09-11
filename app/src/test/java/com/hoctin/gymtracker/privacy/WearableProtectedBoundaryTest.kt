package com.hoctin.gymtracker.privacy

import java.io.File
import java.security.MessageDigest
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

class WearableProtectedBoundaryTest {
    @Test
    fun manifestRequestsExactlyTheThreeV1ReadPermissions() {
        val document = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(projectFile("src/main/AndroidManifest.xml"))
        val namespace = "http://schemas.android.com/apk/res/android"
        val permissions = document.getElementsByTagName("uses-permission")
        val names = buildSet {
            for (index in 0 until permissions.length) {
                add((permissions.item(index) as Element).getAttributeNS(namespace, "name"))
            }
        }

        assertEquals(
            setOf(
                "android.permission.health.READ_SLEEP",
                "android.permission.health.READ_HEART_RATE_VARIABILITY",
                "android.permission.health.READ_RESTING_HEART_RATE",
            ),
            names,
        )
        assertTrue(names.none { it.contains("WRITE_") })
        assertFalse("android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND" in names)
        assertFalse("android.permission.health.READ_HEALTH_DATA_HISTORY" in names)
        assertFalse("android.permission.INTERNET" in names)
    }

    @Test
    fun protectedEnginesHaveNoWearableDependencyOrModifierSurface() {
        listOf(
            "src/main/java/com/hoctin/gymtracker/domain/muscleheat/MuscleRecovery.kt",
            "src/main/java/com/hoctin/gymtracker/domain/muscleheat/MuscleHeatEngine.kt",
            "src/main/java/com/hoctin/gymtracker/domain/ProgressiveOverload.kt",
        ).forEach { path ->
            val source = projectFile(path).readText()
            assertFalse("$path must not import wearable context", source.contains("data.wearable"))
            assertFalse("$path must not expose a wearable modifier", source.contains("Wearable"))
            assertFalse("$path must not expose a Health Connect modifier", source.contains("HealthConnect"))
        }
    }

    @Test
    fun sleepSelectionRepairLeavesProtectedModelBytesFrozen() {
        val expectedHashes = mapOf(
            "src/main/java/com/hoctin/gymtracker/domain/muscleheat/MuscleRecovery.kt" to
                "0520CE83248BE1A5F9431E97981E12F75ECBA77F9262355644B0D8E2F8DBF748",
            "src/main/java/com/hoctin/gymtracker/domain/muscleheat/MuscleHeatEngine.kt" to
                "12804F8654BA8124B2200BFB1066360FA70D5A82058C948FAA3F8DE1EB065341",
            "src/main/java/com/hoctin/gymtracker/domain/ProgressiveOverload.kt" to
                "8D2355D55980B647CE327C7FF790718007FC97360A092886CE6EF5C88CBB957B",
        )

        expectedHashes.forEach { (path, expected) ->
            assertEquals("Protected bytes changed: $path", expected, sha256(projectFile(path)))
        }
    }

    @Test
    fun homeUsesSelectedSessionWordingAndDisclosesOtherEligibleSessions() {
        val source = projectFile(
            "src/main/java/com/hoctin/gymtracker/ui/home/HealthRecoveryContextCard.kt",
        ).readText()

        assertTrue(source.contains("Selected-session sleep"))
        assertTrue(source.contains("otherPriorSleepSessions"))
        assertTrue(source.contains("not included in the selected-session value"))
        assertFalse(source.contains("\"Total sleep\""))
    }

    @Test
    fun frozenV1ExportDoesNotSilentlyIncludeHealthTables() {
        val source = projectFile(
            "src/main/java/com/hoctin/gymtracker/data/research/RecoveryResearchExport.kt",
        ).readText()
        assertFalse(source.contains("wearable_shadow"))
        assertFalse(source.contains("wearable_context"))
        assertFalse(source.contains("HealthConnect"))
    }

    @Test
    fun shadowSnapshotIsAttachedOnlyAfterProtectedPredictionFreeze() {
        val source = projectFile(
            "src/main/java/com/hoctin/gymtracker/ui/log/LogWorkoutViewModel.kt",
        ).readText()
        val freeze = source.indexOf("freezePredictionIfEligible(")
        val shadow = source.indexOf("captureSnapshotForPrediction(")
        assertTrue("Protected prediction freeze must exist", freeze >= 0)
        assertTrue("Shadow capture must follow the freeze", shadow > freeze)
    }

    @Test
    fun dependencyGraphUsesHealthConnectOnly_andNoSamsungSdk() {
        val build = projectFile("build.gradle.kts").readText()
        assertTrue(build.contains("androidx.health.connect"))
        assertFalse(build.contains("com.samsung"))
        assertFalse(build.contains("samsung-health"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("app", path))
        return candidates.firstOrNull(File::exists)
            ?: error("Cannot locate $path from ${File(".").absolutePath}")
    }

    private fun sha256(file: File): String = MessageDigest.getInstance("SHA-256")
        .digest(file.readBytes())
        .joinToString("") { byte -> "%02X".format(byte) }
}
