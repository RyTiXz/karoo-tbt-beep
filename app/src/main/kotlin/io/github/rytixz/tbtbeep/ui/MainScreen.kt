package io.github.rytixz.tbtbeep.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.hammerhead.karooext.KarooSystemService
import kotlinx.coroutines.launch
import io.github.rytixz.tbtbeep.KarooTbtExtension.Companion.TAG
import io.github.rytixz.tbtbeep.R
import io.github.rytixz.tbtbeep.TbtSettings
import io.github.rytixz.tbtbeep.saveSettings
import io.github.rytixz.tbtbeep.streamSettings

@Composable
fun MainScreen() {
    val pattern = remember { Regex("^\\d*$") }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val focusManager = LocalFocusManager.current
    val karooSystem = remember { KarooSystemService(ctx) }
    var savedDialogVisible by remember { mutableStateOf(false) }

    var uiEnabled by remember { mutableStateOf(true) }
    var uiInRideOnly by remember { mutableStateOf(false) }
    var uiWakeUpScreen by remember { mutableStateOf(false) }
    var uiEarlyAlert by remember { mutableStateOf(TbtSettings().earlyAlert) }
    var uiFarAlert by remember { mutableStateOf(TbtSettings().farAlert) }
    var uiNearAlert by remember { mutableStateOf(TbtSettings().nearAlert) }

    fun saveUISettings() {
        scope.launch {
            val settings = TbtSettings(
                enabled = uiEnabled,
                inRideOnly = uiInRideOnly,
                wakeUpScreen = uiWakeUpScreen,
                earlyAlert = uiEarlyAlert,
                farAlert = uiFarAlert,
                nearAlert = uiNearAlert,
            )
            Log.i(TAG, "" + settings)
            saveSettings(ctx, settings)
        }
    }

    LaunchedEffect(Unit) {
        ctx.streamSettings().collect { settings ->
            uiEnabled = settings.enabled
            uiInRideOnly = settings.inRideOnly
            uiWakeUpScreen = settings.wakeUpScreen
            uiEarlyAlert = settings.earlyAlert
            uiFarAlert = settings.farAlert
            uiNearAlert = settings.nearAlert
        }
    }

    LaunchedEffect(Unit) {
        karooSystem.connect()
    }

    @Composable
    fun labelledSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                modifier = Modifier
                    .weight(0.5f)
                    .padding(5.dp),
                checked = checked,
                onCheckedChange = onChange,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(modifier = Modifier.weight(1f), text = label)
        }
    }

    @Composable
    fun helpLine(text: String) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.bodySmall,
            text = text,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxSize()
            .padding(2.dp)
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .clickable { focusManager.clearFocus() },
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(5.dp)
        ) {}
        DrawAlertPanel(
            karooSystem, scope, stringResource(R.string.early_alert), uiEarlyAlert, pattern,
            onChange = { changed ->
                val toggled = changed.enabled != uiEarlyAlert.enabled
                uiEarlyAlert = changed
                // Der An/Aus-Schalter speichert sofort, Zahlenfelder erst via Save
                if (toggled) saveUISettings()
            },
        )
        HorizontalDivider(
            thickness = 2.dp, modifier = Modifier.padding(vertical = 8.dp)
        )
        DrawAlertPanel(
            karooSystem, scope, stringResource(R.string.approach_alert), uiFarAlert, pattern,
            onChange = { changed ->
                val toggled = changed.enabled != uiFarAlert.enabled
                uiFarAlert = changed
                if (toggled) saveUISettings()
            },
        )
        HorizontalDivider(
            thickness = 2.dp, modifier = Modifier.padding(vertical = 8.dp)
        )
        DrawAlertPanel(
            karooSystem, scope, stringResource(R.string.at_turn_alert), uiNearAlert, pattern,
            onChange = { changed ->
                val toggled = changed.enabled != uiNearAlert.enabled
                uiNearAlert = changed
                if (toggled) saveUISettings()
            },
        )
        HorizontalDivider(
            thickness = 2.dp, modifier = Modifier.padding(vertical = 8.dp)
        )
        labelledSwitch(stringResource(R.string.enabled), uiEnabled) {
            uiEnabled = it
            saveUISettings()
        }
        labelledSwitch(stringResource(R.string.in_ride_only), uiInRideOnly) {
            uiInRideOnly = it
            saveUISettings()
        }
        labelledSwitch(stringResource(R.string.wake_up_screen), uiWakeUpScreen) {
            uiWakeUpScreen = it
            saveUISettings()
        }
        Spacer(modifier = Modifier.size(10.dp))
        FilledTonalButton(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            onClick = {
                scope.launch {
                    saveUISettings()
                    savedDialogVisible = true
                }
            }) {
            Icon(Icons.Default.Done, contentDescription = null)
            Spacer(modifier = Modifier.width(5.dp))
            Text(stringResource(R.string.save))
        }
        HorizontalDivider(
            thickness = 2.dp, modifier = Modifier.padding(vertical = 8.dp)
        )
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            style = MaterialTheme.typography.titleSmall,
            text = stringResource(R.string.help_title),
        )
        helpLine(stringResource(R.string.help_alerts))
        helpLine(stringResource(R.string.help_meters))
        helpLine(stringResource(R.string.help_frequency))
        helpLine(stringResource(R.string.help_duration))
        helpLine(stringResource(R.string.help_beeps))
        helpLine(stringResource(R.string.help_enabled))
        helpLine(stringResource(R.string.help_in_ride))
        helpLine(stringResource(R.string.help_wake))
        helpLine(stringResource(R.string.help_stock))
        Spacer(modifier = Modifier.size(10.dp))
        if (savedDialogVisible) {
            AlertDialog(
                onDismissRequest = { savedDialogVisible = false },
                confirmButton = {
                    Button(onClick = {
                        savedDialogVisible = false
                    }) { Text(stringResource(R.string.ok)) }
                },
                text = { Text(stringResource(R.string.settings_saved)) }
            )
        }
    }
}
