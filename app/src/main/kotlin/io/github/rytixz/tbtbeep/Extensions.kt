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

// encodeDefaults: Settings immer als Voll-Snapshot speichern, damit spaetere
// Default-Aenderungen gespeicherte User-Werte nie stillschweigend veraendern
val jsonWithUnknownKeys = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

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

fun KarooSystemService.playBeep(beep: Beep) {
    val tones = beep.tones().map { (freq, dur) -> PlayBeepPattern.Tone(freq, dur) }
    if (tones.isEmpty()) return
    dispatch(PlayBeepPattern(tones))
}
