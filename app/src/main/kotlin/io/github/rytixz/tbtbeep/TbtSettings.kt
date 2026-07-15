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
    val farAlert: TurnAlert = TurnAlert(100, Beep(800, 100, 1)),
    val nearAlert: TurnAlert = TurnAlert(20, Beep(800, 100, 2)),
) {
    companion object {
        val defaultSettings = jsonWithUnknownKeys.encodeToString(TbtSettings())
    }
}
