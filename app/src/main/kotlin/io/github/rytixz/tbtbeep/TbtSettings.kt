package io.github.rytixz.tbtbeep

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Serializable
data class Beep(
    val frequency: Int = 800,
    val duration: Int = 100,
    val count: Int = 1,
)

@Serializable
data class TurnAlert(
    // Distanz zum Turn in Metern, bei deren Unterschreitung der Alert feuert
    val distance: Int,
    val beep: Beep,
    val enabled: Boolean = true,
)

@Serializable
data class TbtSettings(
    val enabled: Boolean = true,
    val inRideOnly: Boolean = false,
    val wakeUpScreen: Boolean = false,
    val earlyAlert: TurnAlert = TurnAlert(1000, Beep(800, 100, 3)),
    val farAlert: TurnAlert = TurnAlert(250, Beep(800, 100, 2)),
    val nearAlert: TurnAlert = TurnAlert(50, Beep(800, 100, 1)),
) {
    companion object {
        val defaultSettings = jsonWithUnknownKeys.encodeToString(TbtSettings())
    }
}
