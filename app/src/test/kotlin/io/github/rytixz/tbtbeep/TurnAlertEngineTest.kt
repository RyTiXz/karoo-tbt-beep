package io.github.rytixz.tbtbeep

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TurnAlertEngineTest {
    private val far = TurnAlert(100, Beep(800, 100, 1), enabled = true)
    private val near = TurnAlert(20, Beep(800, 100, 2), enabled = true)
    private val settings = TbtSettings(
        enabled = true,
        farAlert = far,
        nearAlert = near,
    )

    @Test
    fun `far alert fires exactly once while approaching`() {
        val engine = TurnAlertEngine()
        assertNull(engine.onDistance(500.0, settings))
        assertEquals(far, engine.onDistance(95.0, settings))
        assertNull(engine.onDistance(80.0, settings))
        assertNull(engine.onDistance(50.0, settings))
    }

    @Test
    fun `near alert fires and suppresses pending far alert`() {
        val engine = TurnAlertEngine()
        engine.onDistance(500.0, settings)
        assertEquals(near, engine.onDistance(15.0, settings))
        assertNull(engine.onDistance(10.0, settings))
    }

    @Test
    fun `both alerts fire in sequence for one turn`() {
        val engine = TurnAlertEngine()
        engine.onDistance(500.0, settings)
        assertEquals(far, engine.onDistance(90.0, settings))
        assertEquals(near, engine.onDistance(18.0, settings))
        assertNull(engine.onDistance(5.0, settings))
    }

    @Test
    fun `re-arms after turn passed (distance jumps up)`() {
        val engine = TurnAlertEngine()
        engine.onDistance(500.0, settings)
        engine.onDistance(90.0, settings)
        engine.onDistance(15.0, settings)
        assertNull(engine.onDistance(400.0, settings))
        assertEquals(far, engine.onDistance(99.0, settings))
    }

    @Test
    fun `re-arms after sharp drop (reroute)`() {
        val engine = TurnAlertEngine()
        engine.onDistance(600.0, settings)
        assertNull(engine.onDistance(400.0, settings))
        assertEquals(far, engine.onDistance(95.0, settings))
    }

    @Test
    fun `small gps jitter does not re-arm`() {
        val engine = TurnAlertEngine()
        engine.onDistance(500.0, settings)
        assertEquals(far, engine.onDistance(95.0, settings))
        assertNull(engine.onDistance(110.0, settings))
        assertNull(engine.onDistance(94.0, settings))
    }

    @Test
    fun `master disabled stays silent but keeps tracking`() {
        val engine = TurnAlertEngine()
        val disabled = settings.copy(enabled = false)
        assertNull(engine.onDistance(500.0, disabled))
        assertNull(engine.onDistance(95.0, disabled))
        assertEquals(near, engine.onDistance(15.0, settings))
    }

    @Test
    fun `disabled far alert only fires near`() {
        val engine = TurnAlertEngine()
        val farOff = settings.copy(farAlert = far.copy(enabled = false))
        engine.onDistance(500.0, farOff)
        assertNull(engine.onDistance(95.0, farOff))
        assertEquals(near, engine.onDistance(19.0, farOff))
    }

    @Test
    fun `consecutive close turns do not catch up missed thresholds`() {
        val engine = TurnAlertEngine()
        engine.onDistance(500.0, settings)
        assertEquals(far, engine.onDistance(90.0, settings))
        assertEquals(near, engine.onDistance(15.0, settings))
        // Naechster Turn taucht bereits innerhalb der Far-Schwelle auf (80 m < 100 m):
        // Far gilt als verpasst und darf nicht nachgeholt werden
        assertNull(engine.onDistance(80.0, settings))
        assertNull(engine.onDistance(50.0, settings))
        assertEquals(near, engine.onDistance(18.0, settings))
    }

    @Test
    fun `navigation starting inside far zone skips far alert`() {
        val engine = TurnAlertEngine()
        assertNull(engine.onDistance(60.0, settings))
        assertEquals(near, engine.onDistance(15.0, settings))
    }

    @Test
    fun `navigation starting inside near zone stays silent for that turn`() {
        val engine = TurnAlertEngine()
        assertNull(engine.onDistance(10.0, settings))
        assertNull(engine.onDistance(5.0, settings))
        assertNull(engine.onDistance(400.0, settings))
        assertEquals(far, engine.onDistance(95.0, settings))
    }

    @Test
    fun `early alert fires before far and near when enabled`() {
        val engine = TurnAlertEngine()
        val early = TurnAlert(1000, Beep(800, 100, 3), enabled = true)
        val withEarly = settings.copy(earlyAlert = early)
        assertNull(engine.onDistance(1500.0, withEarly))
        assertEquals(early, engine.onDistance(950.0, withEarly))
        assertNull(engine.onDistance(800.0, withEarly))
        assertEquals(far, engine.onDistance(95.0, withEarly))
        assertEquals(near, engine.onDistance(15.0, withEarly))
    }

    @Test
    fun `early alert disabled by default stays silent`() {
        val engine = TurnAlertEngine()
        assertNull(engine.onDistance(1500.0, settings))
        assertNull(engine.onDistance(900.0, settings))
        assertEquals(far, engine.onDistance(95.0, settings))
    }

    @Test
    fun `invalid distances are ignored`() {
        val engine = TurnAlertEngine()
        assertNull(engine.onDistance(Double.NaN, settings))
        assertNull(engine.onDistance(-5.0, settings))
        engine.onDistance(500.0, settings)
        assertEquals(far, engine.onDistance(95.0, settings))
    }
}
