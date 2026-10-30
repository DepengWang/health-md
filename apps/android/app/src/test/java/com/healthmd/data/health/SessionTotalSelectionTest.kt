package com.healthmd.data.health

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionTotalSelectionTest {
    private data class Rec(val start: Instant, val end: Instant, val amount: Double)

    private val sessionStart = Instant.parse("2026-10-30T12:51:47Z")
    private val sessionEnd = Instant.parse("2026-10-30T13:54:01Z")

    private fun List<Rec>.select() =
        sessionTotalOrAll(sessionStart, sessionEnd, { it.start }, { it.end })

    private fun perMinute(count: Int, meters: Double) = (0 until count).map {
        val s = Instant.parse("2026-10-30T12:51:00Z").plusSeconds(it * 60L)
        Rec(s, s.plusSeconds(60), meters)
    }

    @Test
    fun `workout total record replaces per-minute samples`() {
        val total = Rec(sessionStart, sessionEnd, 10240.0)
        val records = perMinute(64, 161.5) + total
        assertEquals(listOf(total), records.select())
    }

    @Test
    fun `total within a minute of the bounds still counts`() {
        val total = Rec(sessionStart.minusSeconds(40), sessionEnd.plusSeconds(30), 10240.0)
        assertEquals(listOf(total), (perMinute(5, 100.0) + total).select())
    }

    @Test
    fun `per-minute samples are all kept when no record matches the workout`() {
        val records = perMinute(10, 100.0)
        assertEquals(records, records.select())
    }

    @Test
    fun `day-long record is not treated as the workout total`() {
        val day = Rec(Instant.parse("2026-10-30T00:00:00Z"), Instant.parse("2026-10-31T00:00:00Z"), 15000.0)
        val records = perMinute(3, 100.0) + day
        assertEquals(records, records.select())
    }

    @Test
    fun `a lone record is returned as is`() {
        val only = Rec(sessionStart, sessionEnd, 10240.0)
        assertEquals(listOf(only), listOf(only).select())
    }

    @Test
    fun `empty list stays empty`() {
        assertEquals(emptyList<Rec>(), emptyList<Rec>().select())
    }
}
