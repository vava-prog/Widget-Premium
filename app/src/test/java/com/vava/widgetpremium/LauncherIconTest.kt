package com.vava.widgetpremium

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherIconTest {
    @Test
    fun nightIconOnlyWhenNightAndEnabled() {
        assertTrue(LauncherIcon.wantNight(isDay = false, featureEnabled = true))
        assertFalse(LauncherIcon.wantNight(isDay = true, featureEnabled = true))
        assertFalse(LauncherIcon.wantNight(isDay = false, featureEnabled = false))
    }
}
