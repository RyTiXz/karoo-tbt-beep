package io.github.rytixz.tbtbeep

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TbtSettingsTest {
    @Test
    fun `roundtrip keeps all values`() {
        val settings = TbtSettings(
            enabled = false,
            inRideOnly = true,
            wakeUpScreen = true,
            farAlert = TurnAlert(250, Beep(400, 60, 3), enabled = false),
            nearAlert = TurnAlert(30, Beep(1200, 200, 1), enabled = true),
        )
        val json = jsonWithUnknownKeys.encodeToString(TbtSettings.serializer(), settings)
        val decoded = jsonWithUnknownKeys.decodeFromString<TbtSettings>(json)
        assertEquals(settings, decoded)
    }

    @Test
    fun `unknown fields from future versions are tolerated`() {
        val json = """{"enabled":true,"someFutureField":42}"""
        val decoded = jsonWithUnknownKeys.decodeFromString<TbtSettings>(json)
        assertTrue(decoded.enabled)
        assertEquals(TbtSettings().farAlert, decoded.farAlert)
    }
}
