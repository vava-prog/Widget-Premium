package com.vava.widgetpremium

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationRulesTest {
    @Test fun roundsToTwoDecimals() {
        assertEquals(16.05, LocationRules.round2(16.0544), 1e-9)
        assertEquals(108.21, LocationRules.round2(108.2149), 1e-9)
        assertEquals(-33.87, LocationRules.round2(-33.8688), 1e-9)
    }

    @Test fun validatesCoordinates() {
        assertTrue(LocationRules.isValid(16.05, 108.21))
        assertFalse(LocationRules.isValid(91.0, 10.0))
        assertFalse(LocationRules.isValid(10.0, 181.0))
        assertFalse(LocationRules.isValid(0.0, 0.0))
    }
}
