package com.hoctin.gymtracker.ui.review

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class HeatCardShareTest {

    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun shareFileName_isStableAndPngOnly() {
        assertEquals(
            "auron_heat_session_42_123456.png",
            buildHeatCardShareFileName(sessionId = 42, nowMillis = 123456),
        )
    }

    @Test
    fun exportHeight_usesTallerCardWhenDurationIsVisible() {
        val withoutDuration = heatCardExportHeight(hasDuration = false, footnoteLineCount = 0)
        val withDuration = heatCardExportHeight(hasDuration = true, footnoteLineCount = 0)

        assertTrue(withDuration > withoutDuration)
    }

    @Test
    fun exportHeight_growsForFootnotes() {
        val base = heatCardExportHeight(hasDuration = true, footnoteLineCount = 0)
        val withFootnotes = heatCardExportHeight(hasDuration = true, footnoteLineCount = 3)

        assertTrue(withFootnotes > base)
    }

    @Test
    fun cleanupOldHeatCardShares_deletesOnlyExpiredPngFiles() {
        val directory = temp.newFolder("shared_heat_cards")
        val oldPng = File(directory, "old.png").apply {
            writeText("old")
            setLastModified(1_000L)
        }
        val freshPng = File(directory, "fresh.png").apply {
            writeText("fresh")
            setLastModified(9_500L)
        }
        val oldText = File(directory, "old.txt").apply {
            writeText("old")
            setLastModified(1_000L)
        }

        cleanupOldHeatCardShares(directory, nowMillis = 10_000L, maxAgeMillis = 1_000L)

        assertFalse(oldPng.exists())
        assertTrue(freshPng.exists())
        assertTrue(oldText.exists())
    }
}
