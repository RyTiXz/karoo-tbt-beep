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
    fun `tone patterns produce expected sequences`() {
        val base = Beep(frequency = 800, duration = 100)
        assertEquals(listOf(800 to 100), base.copy(pattern = TonePattern.SINGLE).tones())
        assertEquals(
            listOf(800 to 100, 0 to Beep.GAP_MS, 800 to 100),
            base.copy(pattern = TonePattern.DOUBLE).tones(),
        )
        assertEquals(
            listOf(800 to 100, 0 to Beep.GAP_MS, 1200 to 100),
            base.copy(pattern = TonePattern.UP).tones(),
        )
        assertEquals(
            listOf(1200 to 100, 0 to Beep.GAP_MS, 800 to 100),
            base.copy(pattern = TonePattern.DOWN).tones(),
        )
    }

    @Test
    fun `legacy count maps to pattern without pattern field`() {
        assertEquals(TonePattern.DOUBLE, Beep(count = 2).effectivePattern())
        assertEquals(TonePattern.TRIPLE, Beep(count = 3).effectivePattern())
        // Legacy count > 3 bleibt als Wiederholung erhalten
        assertEquals(5, Beep(count = 5).tones().count { it.first > 0 })
    }

    @Test
    fun `custom sequences are parsed defensively`() {
        assertEquals(
            listOf(800 to 100, 0 to 80, 1200 to 100),
            Beep.parseCustomSequence("800:100, 0:80, 1200:100"),
        )
        assertEquals(emptyList<Pair<Int, Int>>(), Beep.parseCustomSequence("kaputt"))
        assertEquals(listOf(800 to 100), Beep.parseCustomSequence("800:100, unsinn, 500:-20"))
        // Ungueltige Custom-Sequenz faellt auf count-Wiederholung zurueck
        val fallback = Beep(count = 2, pattern = TonePattern.CUSTOM, custom = "xyz").tones()
        assertEquals(2, fallback.count { it.first > 0 })
    }

    @Test
    fun `unknown fields from future versions are tolerated`() {
        val json = """{"enabled":true,"someFutureField":42}"""
        val decoded = jsonWithUnknownKeys.decodeFromString<TbtSettings>(json)
        assertTrue(decoded.enabled)
        assertEquals(TbtSettings().farAlert, decoded.farAlert)
    }
}
