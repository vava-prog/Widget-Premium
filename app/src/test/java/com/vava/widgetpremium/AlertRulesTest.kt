package com.vava.widgetpremium

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertRulesTest {
    private val now = 10 * 60 * 60 * 1000L

    @Test fun lowBatteryOnlyWhenNotChargingAndAtOrBelowThreshold() {
        assertTrue(AlertRules.shouldWarnLow(15, false, 0L, now))
        assertTrue(AlertRules.shouldWarnLow(5, false, 0L, now))
        assertFalse(AlertRules.shouldWarnLow(16, false, 0L, now))
        assertFalse(AlertRules.shouldWarnLow(5, true, 0L, now))
    }

    @Test fun lowBatteryRespectsCooldown() {
        assertFalse(AlertRules.shouldWarnLow(10, false, now - 1000L, now))
        assertTrue(AlertRules.shouldWarnLow(10, false, now - AlertRules.LOW_COOLDOWN_MS, now))
    }

    @Test fun hotNeedsKnownTemperatureAtOrAboveThreshold() {
        assertFalse(AlertRules.shouldWarnHot(null, 0L, now))
        assertFalse(AlertRules.shouldWarnHot(41.9f, 0L, now))
        assertTrue(AlertRules.shouldWarnHot(42f, 0L, now))
        assertFalse(AlertRules.shouldWarnHot(45f, now - 60_000L, now))
        assertTrue(AlertRules.shouldWarnHot(45f, now - AlertRules.HOT_COOLDOWN_MS, now))
    }
}
