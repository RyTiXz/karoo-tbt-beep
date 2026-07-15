package io.github.rytixz.tbtbeep

/**
 * Pure threshold engine, independent of Android/Karoo so it can be unit-tested.
 *
 * Tracks the distance-to-next-turn stream and decides which alert (if any)
 * fires. Each alert fires at most once per turn; flags re-arm when the
 * distance jumps up (turn passed / next turn) or drops sharply (reroute).
 */
class TurnAlertEngine {
    companion object {
        const val RESET_JUMP_M = 50.0
        const val REROUTE_DROP_M = 150.0
    }

    private var lastDistance: Double? = null
    private var firedFar = false
    private var firedNear = false

    fun onDistance(distance: Double, settings: TbtSettings): TurnAlert? {
        if (distance.isNaN() || distance < 0) return null

        lastDistance?.let { last ->
            if (distance > last + RESET_JUMP_M || last - distance > REROUTE_DROP_M) {
                firedFar = false
                firedNear = false
            }
        }
        lastDistance = distance

        if (!settings.enabled) return null

        val far = settings.farAlert
        val near = settings.nearAlert

        return when {
            near.enabled && !firedNear && distance <= near.distance -> {
                firedNear = true
                firedFar = true
                near
            }
            far.enabled && !firedFar && distance <= far.distance -> {
                firedFar = true
                far
            }
            else -> null
        }
    }
}
