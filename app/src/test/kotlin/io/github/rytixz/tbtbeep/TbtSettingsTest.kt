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
    fun `settings are persisted as full snapshot including default values`() {
        // Schutz gegen stilles Umschreiben von User-Settings durch spaetere
        // Default-Aenderungen: auch Default-Werte muessen im JSON stehen
        val json = jsonWithUnknownKeys.encodeToString(TbtSettings.serializer(), TbtSettings())
        assertTrue(json.contains("earlyAlert"))
        assertTrue(json.contains("frequency"))
        assertTrue(json.contains("enabled"))
    }

    @Test
    fun `unknown fields from future versions are tolerated`() {
        val json = """{"enabled":true,"someFutureField":42}"""
        val decoded = jsonWithUnknownKeys.decodeFromString<TbtSettings>(json)
        assertTrue(decoded.enabled)
        assertEquals(TbtSettings().farAlert, decoded.farAlert)
    }
}
