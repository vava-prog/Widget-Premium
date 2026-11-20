package com.vava.widgetpremium

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatsTest {
    @Test
    fun smallValuesKeepOneDecimal() {
        assertEquals("2.5", Formats.gb(2_500_000_000L, Locale.US))
        assertEquals("5,2", Formats.gb(5_200_000_000L, Locale("vi")))
    }

    @Test
    fun largeValuesAreRounded() {
        assertEquals("128", Formats.gb(128_400_000_000L, Locale.US))
    }

    @Test
    fun celsiusOneDecimal() {
        assertEquals("34.5", Formats.celsius(34.5f, Locale.US))
    }
}
