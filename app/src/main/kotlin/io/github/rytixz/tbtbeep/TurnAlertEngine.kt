package io.github.rytixz.tbtbeep

/**
 * Ergebnis eines Distanz-Samples: sofort feuern (delayMs = 0), zeitversetzt
 * feuern (delayMs > 0, per Geschwindigkeit prognostizierter Schwellen-Moment)
 * oder nichts. cancelPending signalisiert einen Kontextwechsel (Turn passiert,
 * Reroute) — ein noch nicht abgespielter geplanter Beep ist dann hinfaellig.
 */
data class EngineOutput(
    val alert: TurnAlert? = null,
    val delayMs: Long = 0L,
    val cancelPending: Boolean = false,
)

/**
 * Pure threshold engine, independent of Android/Karoo so it can be unit-tested.
 *
 * Tracks the distance-to-next-turn stream and decides which alert (if any)
 * fires. Each alert fires at most once per turn. When a new turn context is
 * detected, alerts are re-armed — but only thresholds still ahead; thresholds
 * the rider is already inside of count as missed and stay silent.
 *
 * With a plausible speed, threshold crossings that fall between two stream
 * updates are scheduled ahead of time (delayMs) instead of firing late on the
 * next update.
 */
class TurnAlertEngine {
    companion object {
        const val RESET_JUMP_M = 50.0
        const val REROUTE_DROP_M = 150.0

        // Vorausschau maximal ein Update-Fenster, sonst korrigiert das naechste Sample
        const val PREDICTION_HORIZON_MS = 1500L

        // Plausibles Rad-Tempo in m/s; ausserhalb wird nicht prognostiziert
        const val MIN_SPEED_MPS = 1.0
        const val MAX_SPEED_MPS = 30.0
    }

    private var lastDistance: Double? = null
    private var firedEarly = true
    private var firedFar = true
    private var firedNear = true

    private fun arm(distance: Double, settings: TbtSettings) {
        // Bereits unterschrittene Schwellen gelten als verpasst und bleiben stumm
        firedEarly = distance <= settings.earlyAlert.distance
        firedFar = distance <= settings.farAlert.distance
        firedNear = distance <= settings.nearAlert.distance
    }

    private fun markFired(alert: TurnAlert, settings: TbtSettings) {
        when (alert) {
            settings.nearAlert -> {
                firedNear = true
                firedFar = true
                firedEarly = true
            }
            settings.farAlert -> {
                firedFar = true
                firedEarly = true
            }
            else -> firedEarly = true
        }
    }

    fun onDistance(distance: Double, speedMps: Double?, settings: TbtSettings): EngineOutput {
        if (distance.isNaN() || distance < 0) return EngineOutput()

        val last = lastDistance
        var cancelPending = false
        if (last == null || distance > last + RESET_JUMP_M || last - distance > REROUTE_DROP_M) {
            arm(distance, settings)
            cancelPending = last != null
        }
        lastDistance = distance

        if (!settings.enabled) return EngineOutput(cancelPending = cancelPending)

        val early = settings.earlyAlert
        val far = settings.farAlert
        val near = settings.nearAlert

        // Sofort-Fall: Schwelle bereits unterschritten
        val immediate = when {
            near.enabled && !firedNear && distance <= near.distance -> near
            far.enabled && !firedFar && distance <= far.distance -> far
            early.enabled && !firedEarly && distance <= early.distance -> early
            else -> null
        }
        if (immediate != null) {
            markFired(immediate, settings)
            return EngineOutput(alert = immediate, cancelPending = cancelPending)
        }

        // Prognose-Fall: naechste Schwelle wird vor dem naechsten Update erreicht
        if (speedMps != null && !speedMps.isNaN() &&
            speedMps in MIN_SPEED_MPS..MAX_SPEED_MPS
        ) {
            var next: TurnAlert? = null
            if (near.enabled && !firedNear && distance > near.distance) next = near
            if (far.enabled && !firedFar && distance > far.distance &&
                far.distance > (next?.distance ?: -1)
            ) next = far
            if (early.enabled && !firedEarly && distance > early.distance &&
                early.distance > (next?.distance ?: -1)
            ) next = early

            next?.let { alert ->
                val etaMs = ((distance - alert.distance) / speedMps * 1000).toLong()
                if (etaMs <= PREDICTION_HORIZON_MS) {
                    markFired(alert, settings)
                    return EngineOutput(alert = alert, delayMs = etaMs, cancelPending = cancelPending)
                }
            }
        }

        return EngineOutput(cancelPending = cancelPending)
    }
}
