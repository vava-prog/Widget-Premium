package com.vava.widgetpremium

import org.junit.Assert.assertEquals
import org.junit.Test

class AgeTest {
    @Test fun underOneMinuteIsNow() {
        assertEquals(Age(Age.Span.NOW, 0), Age.of(0))
        assertEquals(Age(Age.Span.NOW, 0), Age.of(59_999))
        assertEquals(Age(Age.Span.NOW, 0), Age.of(-5_000))
    }

    @Test fun roundsDownToLargestUnit() {
        assertEquals(Age(Age.Span.MINUTES, 1), Age.of(60_000))
        assertEquals(Age(Age.Span.MINUTES, 59), Age.of(59 * 60_000L + 59_000))
        assertEquals(Age(Age.Span.HOURS, 1), Age.of(60 * 60_000L))
        assertEquals(Age(Age.Span.HOURS, 23), Age.of(23 * 3_600_000L + 3_599_000))
        assertEquals(Age(Age.Span.DAYS, 1), Age.of(24 * 3_600_000L))
        assertEquals(Age(Age.Span.DAYS, 3), Age.of(3 * 24 * 3_600_000L + 5_000))
    }
}
