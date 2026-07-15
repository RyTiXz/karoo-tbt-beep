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

class KarooTbtExtension : KarooExtension("tbtbeep", "0.4.1") {
    companion object {
        const val TAG = "tbtbeep"
    }

    private lateinit var karooSystem: KarooSystemService
    private var serviceJob: Job? = null
    private val engine = TurnAlertEngine()

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
                    engine.onDistance(distance, settings)?.let { alert ->
                        fireAlert(alert, rideState, settings)
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
        Log.i(TAG, "Turn alert fired (threshold ${alert.distance}m)")
        if (settings.wakeUpScreen) {
            karooSystem.dispatch(TurnScreenOn)
        }
        karooSystem.beep(alert.beep.frequency, alert.beep.duration, alert.beep.count)
    }
}
