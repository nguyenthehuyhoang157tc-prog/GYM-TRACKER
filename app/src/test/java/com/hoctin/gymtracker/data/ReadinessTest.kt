package com.hoctin.gymtracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pure-JVM verification of the V3.6A [Readiness] tolerant parse — the exact logic import uses to decode
 * old / invalid export files safely (missing or unrecognized -> null = UNKNOWN), and the value export
 * writes (the enum `name`). The Room-backed paths (migration 3->4, save persistence, JSON round-trip
 * through the DAO) are not part of the JVM `testDebugUnitTest` harness; they are covered by the
 * additive migration's review + build + runtime QA (see deliverable notes).
 */
class ReadinessTest {

    @Test fun fromName_validValues_parse() {
        assertEquals(Readiness.TIRED, Readiness.fromName("TIRED"))
        assertEquals(Readiness.NORMAL, Readiness.fromName("NORMAL"))
        assertEquals(Readiness.STRONG, Readiness.fromName("STRONG"))
    }

    @Test fun fromName_isTolerantOfCaseAndWhitespace() {
        assertEquals(Readiness.TIRED, Readiness.fromName("tired"))
        assertEquals(Readiness.STRONG, Readiness.fromName("  Strong  "))
        assertEquals(Readiness.NORMAL, Readiness.fromName("Normal"))
    }

    // Mirrors an old export (no field) or an explicit null -> UNKNOWN.
    @Test fun fromName_nullOrBlank_isNull() {
        assertNull(Readiness.fromName(null))
        assertNull(Readiness.fromName(""))
        assertNull(Readiness.fromName("   "))
    }

    // Mirrors an import with an unrecognized value -> UNKNOWN (never throws).
    @Test fun fromName_invalid_isNull() {
        assertNull(Readiness.fromName("EXHAUSTED"))
        assertNull(Readiness.fromName("5"))
        assertNull(Readiness.fromName("normalish"))
    }

    // Export writes the enum name; import must parse it back to the same value.
    @Test fun roundTrip_enumNameParsesBack() {
        Readiness.entries.forEach { r ->
            assertEquals(r, Readiness.fromName(r.name))
        }
    }
}
