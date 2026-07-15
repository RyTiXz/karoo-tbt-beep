package io.github.rytixz.tbtbeep

/**
 * Pure threshold engine, independent of Android/Karoo so it can be unit-tested.
 *
 * Tracks the distance-to-next-turn stream and decides which alert (if any)
 * fires. Each alert fires at most once per turn. When a new turn context is
 * detected (distance jumps up after passing a turn, drops sharply on reroute,
 * or the very first sample arrives), alerts are re-armed — but only those
 * whose threshold still lies ahead: thresholds the rider is already inside of
 * count as missed and stay silent instead of firing retroactively.
 */
class TurnAlertEngine {
    companion object {
        const val RESET_JUMP_M = 50.0
        const val REROUTE_DROP_M = 150.0
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

    fun onDistance(distance: Double, settings: TbtSettings): TurnAlert? {
        if (distance.isNaN() || distance < 0) return null

        val last = lastDistance
        if (last == null || distance > last + RESET_JUMP_M || last - distance > REROUTE_DROP_M) {
            arm(distance, settings)
        }
        lastDistance = distance

        if (!settings.enabled) return null

        val early = settings.earlyAlert
        val far = settings.farAlert
        val near = settings.nearAlert

        return when {
            near.enabled && !firedNear && distance <= near.distance -> {
                firedNear = true
                firedFar = true
                firedEarly = true
                near
            }
            far.enabled && !firedFar && distance <= far.distance -> {
                firedFar = true
                firedEarly = true
                far
            }
            early.enabled && !firedEarly && distance <= early.distance -> {
                firedEarly = true
                early
            }
            else -> null
        }
    }
}
