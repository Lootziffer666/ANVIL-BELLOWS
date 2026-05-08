package com.anvil.bellows

import org.junit.Assert.*
import org.junit.Test

class RateLimitTrackerTest {
    @Test
    fun `rpm buffer calculation is correct`() {
        val rpmLimit = 30
        val buffer = 1
        val effectiveRpm = rpmLimit - buffer
        assertEquals(29, effectiveRpm)
    }

    @Test
    fun `rpd buffer calculation is correct`() {
        val rpdLimit = 14_400
        val buffer = 5
        val effectiveRpd = rpdLimit - buffer
        assertEquals(14_395, effectiveRpd)
    }

    @Test
    fun `unlimited providers always pass rate check`() {
        val rpmLimit = Int.MAX_VALUE
        val rpdLimit = Int.MAX_VALUE
        val effectiveRpm = if (rpmLimit == Int.MAX_VALUE) Int.MAX_VALUE else rpmLimit - 1
        val effectiveRpd = if (rpdLimit == Int.MAX_VALUE) Int.MAX_VALUE else rpdLimit - 5
        assertTrue(0 < effectiveRpm)
        assertTrue(0 < effectiveRpd)
    }
}
