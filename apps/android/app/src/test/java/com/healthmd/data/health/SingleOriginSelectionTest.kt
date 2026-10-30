package com.healthmd.data.health

import org.junit.Assert.assertEquals
import org.junit.Test

class SingleOriginSelectionTest {
    private data class Rec(val origin: String, val amount: Double)

    private fun List<Rec>.select(preferred: String) =
        singleOrigin(preferred, { it.origin }, { it.amount })

    @Test
    fun `single origin is returned unchanged`() {
        val records = listOf(Rec("sync", 5000.0), Rec("sync", 5290.0))
        assertEquals(records, records.select("other"))
    }

    @Test
    fun `overlapping origins are not summed twice`() {
        val records = listOf(
            Rec("sync", 5000.0), Rec("sync", 5290.0),
            Rec("phone", 4900.0), Rec("phone", 5400.0),
        )
        val total = records.select("sync").sumOf { it.amount }
        assertEquals(10290.0, total, 0.0)
    }

    @Test
    fun `workout origin is preferred over a larger origin`() {
        val records = listOf(Rec("sync", 10000.0), Rec("phone", 12000.0))
        assertEquals(listOf(Rec("sync", 10000.0)), records.select("sync"))
    }

    @Test
    fun `largest origin wins when workout origin has no records`() {
        val records = listOf(Rec("a", 1.0), Rec("b", 5.0), Rec("b", 6.0))
        assertEquals(listOf(Rec("b", 5.0), Rec("b", 6.0)), records.select("missing"))
    }

    @Test
    fun `empty list stays empty`() {
        assertEquals(emptyList<Rec>(), emptyList<Rec>().select("sync"))
    }
}
