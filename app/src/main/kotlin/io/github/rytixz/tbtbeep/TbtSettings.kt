package io.github.rytixz.tbtbeep

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Serializable
enum class TonePattern { SINGLE, DOUBLE, TRIPLE, UP, DOWN, CUSTOM }

@Serializable
data class Beep(
    val frequency: Int = 800,
    val duration: Int = 100,
    val count: Int = 1,
    // null = Legacy-Settings ohne Pattern-Feld: Verhalten ergibt sich aus count
    val pattern: TonePattern? = null,
    val custom: String = "",
) {
    fun effectivePattern(): TonePattern = pattern ?: when (count) {
        1 -> TonePattern.SINGLE
        2 -> TonePattern.DOUBLE
        3 -> TonePattern.TRIPLE
        else -> TonePattern.CUSTOM
    }

    // Tonfolge als (Frequenz, Dauer)-Paare; Frequenz 0 = Pause
    fun tones(): List<Pair<Int, Int>> {
        if (frequency <= 0 || duration <= 0) return emptyList()
        return when (effectivePattern()) {
            TonePattern.SINGLE -> repeated(1)
            TonePattern.DOUBLE -> repeated(2)
            TonePattern.TRIPLE -> repeated(3)
            TonePattern.UP -> listOf(frequency to duration, 0 to GAP_MS, frequency * 3 / 2 to duration)
            TonePattern.DOWN -> listOf(frequency * 3 / 2 to duration, 0 to GAP_MS, frequency to duration)
            TonePattern.CUSTOM -> {
                val parsed = parseCustomSequence(custom)
                parsed.ifEmpty { repeated(count.coerceIn(1, MAX_TONES)) }
            }
        }
    }

    private fun repeated(n: Int): List<Pair<Int, Int>> {
        val list = mutableListOf(frequency to duration)
        repeat(n - 1) {
            list.add(0 to GAP_MS)
            list.add(frequency to duration)
        }
        return list
    }

    companion object {
        const val GAP_MS = 80
        const val MAX_TONES = 10
        const val MAX_TONE_MS = 2000

        // Format: "freq:dauer,0:pause,freq:dauer" — ungueltige Teile werden ignoriert
        fun parseCustomSequence(s: String): List<Pair<Int, Int>> =
            s.split(',', ';')
                .mapNotNull { part ->
                    val bits = part.trim().split(':')
                    if (bits.size != 2) return@mapNotNull null
                    val freq = bits[0].trim().toIntOrNull() ?: return@mapNotNull null
                    val dur = bits[1].trim().toIntOrNull() ?: return@mapNotNull null
                    if (freq < 0 || dur <= 0) return@mapNotNull null
                    freq to dur.coerceAtMost(MAX_TONE_MS)
                }
                .take(MAX_TONES)
    }
}

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
    val earlyAlert: TurnAlert = TurnAlert(1000, Beep(800, 100, 3), enabled = false),
    val farAlert: TurnAlert = TurnAlert(250, Beep(800, 100, 2)),
    val nearAlert: TurnAlert = TurnAlert(50, Beep(800, 100, 1)),
) {
    companion object {
        val defaultSettings = jsonWithUnknownKeys.encodeToString(TbtSettings())
    }
}
