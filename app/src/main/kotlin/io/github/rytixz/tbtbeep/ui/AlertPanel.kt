package io.github.rytixz.tbtbeep.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import io.github.rytixz.tbtbeep.R
import io.github.rytixz.tbtbeep.TurnAlert
import io.github.rytixz.tbtbeep.beep

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

@Composable
fun DrawAlertPanel(
    karooSystem: KarooSystemService,
    scope: CoroutineScope,
    title: String,
    alert: TurnAlert,
    pattern: Regex,
    onChange: (TurnAlert) -> Unit,
) {
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
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        NumberField(
            value = alert.distance,
            label = stringResource(R.string.label_distance),
            pattern = pattern,
            modifier = Modifier.weight(1f),
            onChange = { onChange(alert.copy(distance = it)) },
        )
        NumberField(
            value = alert.beep.count,
            label = stringResource(R.string.label_beeps),
            pattern = pattern,
            modifier = Modifier.weight(1f),
            onChange = { onChange(alert.copy(beep = alert.beep.copy(count = it))) },
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 3.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        NumberField(
            value = alert.beep.frequency,
            label = stringResource(R.string.label_frequency),
            pattern = pattern,
            modifier = Modifier.weight(1f),
            onChange = { onChange(alert.copy(beep = alert.beep.copy(frequency = it))) },
        )
        NumberField(
            value = alert.beep.duration,
            label = stringResource(R.string.label_duration),
            pattern = pattern,
            modifier = Modifier.weight(1f),
            onChange = { onChange(alert.copy(beep = alert.beep.copy(duration = it))) },
        )
    }
    FilledTonalButton(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 3.dp)
            .height(44.dp),
        shape = RoundedCornerShape(8.dp),
        onClick = {
            scope.launch {
                karooSystem.beep(
                    alert.beep.frequency,
                    alert.beep.duration,
                    alert.beep.count,
                )
            }
        }) {
        Icon(Icons.Default.PlayArrow, contentDescription = null)
        Spacer(modifier = Modifier.width(5.dp))
        Text(stringResource(R.string.test_sound))
    }
}
