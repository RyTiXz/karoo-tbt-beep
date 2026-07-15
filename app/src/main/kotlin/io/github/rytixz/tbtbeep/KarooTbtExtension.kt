package io.github.rytixz.tbtbeep

import android.util.Log
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.KarooExtension
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.RideState
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.TurnScreenOn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

class KarooTbtExtension : KarooExtension("tbtbeep", "0.1.0") {
    companion object {
        const val TAG = "tbtbeep"

        // Distanz-Sprung nach oben = Turn passiert / naechster Turn -> Alerts scharf schalten
        private const val RESET_JUMP_M = 50.0

        // Grosser Sprung nach unten = Reroute/neue Route -> ebenfalls scharf schalten
        private const val REROUTE_DROP_M = 150.0
    }

    private lateinit var karooSystem: KarooSystemService
    private var serviceJob: Job? = null

    private var lastDistance: Double? = null
    private var firedFar = false
    private var firedNear = false

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "TBT beep extension initialized")
        karooSystem = KarooSystemService(applicationContext)
        serviceJob = CoroutineScope(Dispatchers.IO).launch {
            karooSystem.connect { connected ->
                if (connected) {
                    Log.i(TAG, "karooSystem connected")
                }
            }
            startMonitoring()
        }
    }

    override fun onDestroy() {
        serviceJob?.cancel()
        serviceJob = null
        karooSystem.disconnect()
        super.onDestroy()
    }

    private suspend fun startMonitoring() {
        val settingsFlow = TbtSettingsService(applicationContext).settings
        val rideStateFlow = karooSystem.streamRideState()

        karooSystem.streamDataFlow(DataType.Type.DISTANCE_TO_NEXT_TURN)
            .mapNotNull { (it as? StreamState.Streaming)?.dataPoint?.values }
            .combine(rideStateFlow) { values, rideState ->
                values to rideState
            }
            .combine(settingsFlow) { (values, rideState), settings ->
                Triple(values, rideState, settings)
            }
            .collect { (values, rideState, settings) ->
                values[DataType.Field.DISTANCE_TO_NEXT_TURN]?.let { distance ->
                    handleTurnDistance(distance, rideState, settings)
                }
            }
    }

    private fun handleTurnDistance(
        distance: Double,
        rideState: RideState,
        settings: TbtSettings,
    ) {
        if (distance < 0) return

        lastDistance?.let { last ->
            if (distance > last + RESET_JUMP_M || last - distance > REROUTE_DROP_M) {
                firedFar = false
                firedNear = false
            }
        }
        lastDistance = distance

        if (!settings.enabled) return

        val far = settings.farAlert
        val near = settings.nearAlert

        if (near.enabled && !firedNear && distance <= near.distance) {
            firedNear = true
            firedFar = true
            fireAlert(near, rideState, settings)
        } else if (far.enabled && !firedFar && distance <= far.distance) {
            firedFar = true
            fireAlert(far, rideState, settings)
        }
    }

    private fun fireAlert(
        alert: TurnAlert,
        rideState: RideState,
        settings: TbtSettings,
    ) {
        val allowed = !settings.inRideOnly || rideState is RideState.Recording
        if (!allowed) return
        Log.i(TAG, "Turn alert fired (threshold ${alert.distance}m)")
        if (settings.wakeUpScreen) {
            karooSystem.dispatch(TurnScreenOn)
        }
        karooSystem.beep(alert.beep.frequency, alert.beep.duration, alert.beep.count)
    }
}
