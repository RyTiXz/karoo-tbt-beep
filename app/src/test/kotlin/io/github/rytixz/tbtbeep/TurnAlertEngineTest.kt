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
    fun `invalid distances are ignored`() {
        val engine = TurnAlertEngine()
        assertNull(engine.onDistance(Double.NaN, settings))
        assertNull(engine.onDistance(-5.0, settings))
        engine.onDistance(500.0, settings)
        assertEquals(far, engine.onDistance(95.0, settings))
    }
}
