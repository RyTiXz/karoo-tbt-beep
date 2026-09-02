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
import java.util.concurrent.atomic.AtomicReference

class KarooTbtExtension : KarooExtension("tbtbeep", "0.5.1") {
    companion object {
        const val TAG = "tbtbeep"
    }

    private lateinit var karooSystem: KarooSystemService
    private var serviceJob: Job? = null
    private val engine = TurnAlertEngine()

    // Speed arrives on a collector coroutine, the engine reads it on another.
    // AtomicReference guarantees the latest published value is visible to all readers.
    private val lastSpeedMps = AtomicReference<Double?>(null)

    // Latest settings snapshot; pending beeps re-read it at fire time so a
    // settings change during the prediction window is respected.
    @Volatile
    private var latestSettings: TbtSettings? = null
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
            .collect { lastSpeedMps.set(it) }
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
                latestSettings = settings
                values[DataType.Field.DISTANCE_TO_NEXT_TURN]?.let { distance ->
                    val output = engine.onDistance(distance, lastSpeedMps.get(), settings)
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
        // Re-read settings at fire time: a scheduled (predicted) beep may fire
        // after the user changed settings during the prediction window.
        val current = latestSettings ?: settings
        val allowed = !current.inRideOnly || rideState is RideState.Recording
        if (!allowed) return
        Log.i(TAG, "Turn alert fired (threshold ${alert.distance}m, speed=${lastSpeedMps.get()})")
        if (current.wakeUpScreen) {
            karooSystem.dispatch(TurnScreenOn)
        }
        karooSystem.playBeep(alert.beep)
    }
}
