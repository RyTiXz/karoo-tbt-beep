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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

class KarooTbtExtension : KarooExtension("tbtbeep", "0.5.0") {
    companion object {
        const val TAG = "tbtbeep"
    }

    private lateinit var karooSystem: KarooSystemService
    private var serviceJob: Job? = null
    private val engine = TurnAlertEngine()

    @Volatile
    private var lastSpeedMps: Double? = null
    private var pendingBeep: Job? = null

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
            coroutineScope {
                launch { monitorSpeed() }
                launch { monitorTurnDistance() }
            }
        }
    }

    override fun onDestroy() {
        pendingBeep?.cancel()
        serviceJob?.cancel()
        serviceJob = null
        karooSystem.disconnect()
        super.onDestroy()
    }

    private suspend fun monitorSpeed() {
        karooSystem.streamDataFlow(DataType.Type.SPEED)
            .mapNotNull { (it as? StreamState.Streaming)?.dataPoint?.values?.get(DataType.Field.SPEED) }
            .collect { lastSpeedMps = it }
    }

    private suspend fun CoroutineScope.monitorTurnDistance() {
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
                    val output = engine.onDistance(distance, lastSpeedMps, settings)
                    if (output.cancelPending) {
                        pendingBeep?.cancel()
                        pendingBeep = null
                    }
                    output.alert?.let { alert ->
                        if (output.delayMs <= 0) {
                            fireAlert(alert, rideState, settings)
                        } else {
                            pendingBeep?.cancel()
                            pendingBeep = launch {
                                delay(output.delayMs)
                                fireAlert(alert, rideState, settings)
                            }
                        }
                    }
                }
            }
    }

    private fun fireAlert(
        alert: TurnAlert,
        rideState: RideState,
        settings: TbtSettings,
    ) {
        val allowed = !settings.inRideOnly || rideState is RideState.Recording
        if (!allowed) return
        Log.i(TAG, "Turn alert fired (threshold ${alert.distance}m, speed=$lastSpeedMps)")
        if (settings.wakeUpScreen) {
            karooSystem.dispatch(TurnScreenOn)
        }
        karooSystem.playBeep(alert.beep)
    }
}
