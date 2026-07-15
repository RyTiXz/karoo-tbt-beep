// Stream/Beep-Helper nach dem Vorbild von kxradar (https://github.com/itxsvv/kxradar), Apache-2.0
package io.github.rytixz.tbtbeep

import android.content.Context
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.models.OnStreamState
import io.hammerhead.karooext.models.PlayBeepPattern
import io.hammerhead.karooext.models.RideState
import io.hammerhead.karooext.models.StreamState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.Json

val jsonWithUnknownKeys = Json { ignoreUnknownKeys = true }

suspend fun saveSettings(context: Context, settings: TbtSettings) {
    TbtSettingsService(context).save(settings)
}

fun Context.streamSettings(): Flow<TbtSettings> {
    return TbtSettingsService(this).settings
}

fun KarooSystemService.streamDataFlow(dataTypeId: String): Flow<StreamState> {
    return callbackFlow {
        val listenerId =
            addConsumer(OnStreamState.StartStreaming(dataTypeId)) { event: OnStreamState ->
                trySendBlocking(event.state)
            }
        awaitClose {
            removeConsumer(listenerId)
        }
    }
}

fun KarooSystemService.streamRideState(): Flow<RideState> {
    return callbackFlow {
        val listenerId = addConsumer { rideState: RideState ->
            trySendBlocking(rideState)
        }
        awaitClose {
            removeConsumer(listenerId)
        }
    }
}

fun KarooSystemService.beep(freq: Int, duration: Int, count: Int = 1) {
    if (freq <= 0 || duration <= 0 || count <= 0) return
    val beepList = mutableListOf(PlayBeepPattern.Tone(freq, duration))
    repeat(count - 1) {
        beepList.add(PlayBeepPattern.Tone(0, 80))
        beepList.add(PlayBeepPattern.Tone(freq, duration))
    }
    dispatch(PlayBeepPattern(beepList))
}
