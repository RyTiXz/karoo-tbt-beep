package io.github.rytixz.tbtbeep.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.hammerhead.karooext.KarooSystemService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import io.github.rytixz.tbtbeep.Beep
import io.github.rytixz.tbtbeep.R
import io.github.rytixz.tbtbeep.TonePattern
import io.github.rytixz.tbtbeep.TurnAlert
import io.github.rytixz.tbtbeep.playBeep

private const val FREQ_MIN = 200f
private const val FREQ_MAX = 3000f

@Composable
private fun patternLabel(pattern: TonePattern): String = when (pattern) {
    TonePattern.SINGLE -> stringResource(R.string.pattern_single)
    TonePattern.DOUBLE -> stringResource(R.string.pattern_double)
    TonePattern.TRIPLE -> stringResource(R.string.pattern_triple)
    TonePattern.UP -> stringResource(R.string.pattern_up)
    TonePattern.DOWN -> stringResource(R.string.pattern_down)
    TonePattern.CUSTOM -> stringResource(R.string.pattern_custom)
}

// Beim Preset-Wechsel count synchron halten (Legacy-Kompatibilitaet)
private fun Beep.withPattern(newPattern: TonePattern): Beep = copy(
    pattern = newPattern,
    count = when (newPattern) {
        TonePattern.SINGLE -> 1
        TonePattern.DOUBLE -> 2
        TonePattern.TRIPLE -> 3
        else -> count
    },
)

@Composable
private fun NumberField(
    value: Int,
    label: String,
    pattern: Regex,
    modifier: Modifier = Modifier,
    onChange: (Int) -> Unit,
) {
    var text by remember { mutableStateOf(value.toString()) }

    LaunchedEffect(value) {
        if (text != value.toString()) {
            text = value.toString()
        }
    }

    OutlinedTextField(
        value = text,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        onValueChange = { new ->
            if (new.matches(pattern)) {
                text = new
                onChange(new.toIntOrNull() ?: 0)
            }
        },
        modifier = modifier,
        singleLine = true,
        label = { Text(text = label, maxLines = 1, softWrap = false) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawAlertPanel(
    karooSystem: KarooSystemService,
    scope: CoroutineScope,
    title: String,
    alert: TurnAlert,
    pattern: Regex,
    onChange: (TurnAlert) -> Unit,
) {
    var patternDropdownExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Switch(
            checked = alert.enabled,
            onCheckedChange = { onChange(alert.copy(enabled = it)) }
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = title)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NumberField(
            value = alert.distance,
            label = stringResource(R.string.label_distance),
            pattern = pattern,
            modifier = Modifier.weight(1f),
            onChange = { onChange(alert.copy(distance = it)) },
        )
        ExposedDropdownMenuBox(
            expanded = patternDropdownExpanded,
            onExpandedChange = { patternDropdownExpanded = !patternDropdownExpanded },
            modifier = Modifier.weight(1.3f),
        ) {
            OutlinedTextField(
                value = patternLabel(alert.beep.effectivePattern()),
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                label = { Text(stringResource(R.string.pattern_label), maxLines = 1, softWrap = false) },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = patternDropdownExpanded)
                },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                    .fillMaxWidth(),
            )
            ExposedDropdownMenu(
                expanded = patternDropdownExpanded,
                onDismissRequest = { patternDropdownExpanded = false },
            ) {
                TonePattern.entries.forEach { p ->
                    DropdownMenuItem(
                        text = { Text(patternLabel(p)) },
                        onClick = {
                            patternDropdownExpanded = false
                            onChange(alert.copy(beep = alert.beep.withPattern(p)))
                        },
                    )
                }
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp),
    ) {
        Text(text = "${alert.beep.frequency} Hz")
        Slider(
            value = alert.beep.frequency.toFloat().coerceIn(FREQ_MIN, FREQ_MAX),
            valueRange = FREQ_MIN..FREQ_MAX,
            onValueChange = { v ->
                val rounded = (v / 10).toInt() * 10
                onChange(alert.copy(beep = alert.beep.copy(frequency = rounded)))
            },
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 3.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NumberField(
            value = alert.beep.duration,
            label = stringResource(R.string.label_duration),
            pattern = pattern,
            modifier = Modifier.weight(1f),
            onChange = { onChange(alert.copy(beep = alert.beep.copy(duration = it))) },
        )
        FilledTonalButton(
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(8.dp),
            onClick = {
                scope.launch {
                    karooSystem.playBeep(alert.beep)
                }
            }) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(5.dp))
            Text(stringResource(R.string.test_sound), maxLines = 1, softWrap = false)
        }
    }
    if (alert.beep.effectivePattern() == TonePattern.CUSTOM) {
        var customText by remember { mutableStateOf(alert.beep.custom) }
        LaunchedEffect(alert.beep.custom) {
            if (customText != alert.beep.custom) {
                customText = alert.beep.custom
            }
        }
        OutlinedTextField(
            value = customText,
            onValueChange = { new ->
                customText = new
                onChange(alert.copy(beep = alert.beep.copy(custom = new)))
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 3.dp),
            singleLine = true,
            label = { Text(stringResource(R.string.custom_label), maxLines = 1, softWrap = false) },
            placeholder = { Text("800:100,0:80,1200:100") },
        )
    }
}
