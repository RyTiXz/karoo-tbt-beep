package io.github.rytixz.tbtbeep

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TurnAlertEngineTest {
    private val far = TurnAlert(100, Beep(800, 100, 2), enabled = true)
    private val near = TurnAlert(20, Beep(800, 100, 1), enabled = true)
    private val settings = TbtSettings(
        enabled = true,
        farAlert = far,
        nearAlert = near,
    )

    // Realistische Annaeherung: Schritte klein genug, um weder den
    // Turn-Passed-Jump (+50 m) noch den Reroute-Drop (-150 m) auszuloesen
    private fun TurnAlertEngine.approach(s: TbtSettings, vararg distances: Double): List<TurnAlert> =
        distances.mapNotNull { onDistance(it, s) }

    @Test
    fun `far alert fires exactly once while approaching`() {
        val engine = TurnAlertEngine()
        val fired = engine.approach(settings, 500.0, 380.0, 260.0, 140.0, 95.0, 80.0, 50.0)
        assertEquals(listOf(far), fired)
    }

    @Test
    fun `both alerts fire in sequence for one turn`() {
        val engine = TurnAlertEngine()
        val fired = engine.approach(settings, 500.0, 380.0, 260.0, 140.0, 95.0, 60.0, 18.0, 5.0)
        assertEquals(listOf(far, near), fired)
    }

    @Test
    fun `re-arms after turn passed (distance jumps up)`() {
        val engine = TurnAlertEngine()
        engine.approach(settings, 500.0, 380.0, 260.0, 140.0, 95.0, 60.0, 18.0)
        // Turn passiert, naechster Turn weit weg -> Sprung nach oben
        val fired = engine.approach(settings, 400.0, 280.0, 160.0, 99.0)
        assertEquals(listOf(far), fired)
    }

    @Test
    fun `re-arm after reroute drop keeps future thresholds armed`() {
        val engine = TurnAlertEngine()
        engine.approach(settings, 500.0, 400.0)
        // Reroute: Distanz faellt schlagartig um mehr als 150 m, bleibt aber vor der Schwelle
        val fired = engine.approach(settings, 220.0, 120.0, 95.0, 18.0)
        assertEquals(listOf(far, near), fired)
    }

    @Test
    fun `reroute drop into a zone skips that threshold`() {
        val engine = TurnAlertEngine()
        engine.approach(settings, 500.0, 400.0, 300.0)
        // Reroute direkt in die Far-Zone (60 m): Far gilt als verpasst, Near kommt noch
        val fired = engine.approach(settings, 60.0, 15.0)
        assertEquals(listOf(near), fired)
    }

    @Test
    fun `small gps jitter does not re-arm`() {
        val engine = TurnAlertEngine()
        val first = engine.approach(settings, 500.0, 380.0, 260.0, 140.0, 95.0)
        assertEquals(listOf(far), first)
        val jitter = engine.approach(settings, 110.0, 94.0)
        assertEquals(emptyList<TurnAlert>(), jitter)
    }

    @Test
    fun `consecutive close turns do not catch up missed thresholds`() {
        val engine = TurnAlertEngine()
        engine.approach(settings, 500.0, 380.0, 260.0, 140.0, 95.0, 60.0, 15.0)
        // Naechster Turn taucht bereits innerhalb der Far-Schwelle auf (80 m < 100 m):
        // Far gilt als verpasst und darf nicht nachgeholt werden
        val fired = engine.approach(settings, 80.0, 50.0, 18.0)
        assertEquals(listOf(near), fired)
    }

    @Test
    fun `navigation starting inside far zone skips far alert`() {
        val engine = TurnAlertEngine()
        val fired = engine.approach(settings, 60.0, 40.0, 15.0)
        assertEquals(listOf(near), fired)
    }

    @Test
    fun `navigation starting inside near zone stays silent for that turn`() {
        val engine = TurnAlertEngine()
        assertNull(engine.onDistance(10.0, settings))
        assertNull(engine.onDistance(5.0, settings))
        // Naechster Turn: normal angekuendigt
        val fired = engine.approach(settings, 400.0, 280.0, 160.0, 95.0)
        assertEquals(listOf(far), fired)
    }

    @Test
    fun `early alert fires before far and near when enabled`() {
        val engine = TurnAlertEngine()
        val early = TurnAlert(1000, Beep(800, 100, 3), enabled = true)
        val withEarly = settings.copy(earlyAlert = early)
        val fired = engine.approach(
            withEarly,
            1500.0, 1360.0, 1220.0, 1080.0, 950.0,
            810.0, 670.0, 530.0, 390.0, 250.0, 110.0, 95.0, 60.0, 18.0,
        )
        assertEquals(listOf(early, far, near), fired)
    }

    @Test
    fun `early alert disabled by default stays silent`() {
        val engine = TurnAlertEngine()
        val fired = engine.approach(settings, 1100.0, 950.0, 810.0, 670.0, 530.0, 390.0, 250.0, 110.0, 95.0)
        assertEquals(listOf(far), fired)
    }

    @Test
    fun `master disabled stays silent but keeps tracking`() {
        val engine = TurnAlertEngine()
        val disabled = settings.copy(enabled = false)
        val silent = engine.approach(disabled, 500.0, 380.0, 260.0, 140.0, 95.0)
        assertEquals(emptyList<TurnAlert>(), silent)
        // Wieder aktiviert: Near-Schwelle ist noch scharf
        assertEquals(near, engine.onDistance(15.0, settings))
    }

    @Test
    fun `disabled far alert only fires near`() {
        val engine = TurnAlertEngine()
        val farOff = settings.copy(farAlert = far.copy(enabled = false))
        val fired = engine.approach(farOff, 500.0, 380.0, 260.0, 140.0, 95.0, 60.0, 19.0)
        assertEquals(listOf(near), fired)
    }

    @Test
    fun `invalid distances are ignored`() {
        val engine = TurnAlertEngine()
        assertNull(engine.onDistance(Double.NaN, settings))
        assertNull(engine.onDistance(-5.0, settings))
        val fired = engine.approach(settings, 500.0, 380.0, 260.0, 140.0, 95.0)
        assertEquals(listOf(far), fired)
    }
}
