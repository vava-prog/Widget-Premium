package com.vava.widgetpremium

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetTapTest {
    @Test fun parseFallsBackToHome() {
        assertEquals(TapTarget.BATTERY, TapTarget.parse("BATTERY"))
        assertEquals(TapTarget.HOME, TapTarget.parse(null))
        assertEquals(TapTarget.HOME, TapTarget.parse("khong-co"))
    }

    @Test fun systemTargetsHaveActionsAndAppTargetsDoNot() {
        assertEquals(listOf("android.intent.action.SHOW_ALARMS"), WidgetTap.actionsFor(TapTarget.ALARMS))
        assertTrue(WidgetTap.actionsFor(TapTarget.BATTERY).size >= 2)
        assertTrue(WidgetTap.actionsFor(TapTarget.STORAGE).contains("android.settings.INTERNAL_STORAGE_SETTINGS"))
        assertTrue(WidgetTap.actionsFor(TapTarget.HOME).isEmpty())
        assertTrue(WidgetTap.actionsFor(TapTarget.CALENDAR).isEmpty())
    }
}
