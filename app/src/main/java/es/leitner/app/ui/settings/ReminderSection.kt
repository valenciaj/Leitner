package es.leitner.app.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import es.leitner.app.data.AppPrefs
import es.leitner.app.reminder.ReminderScheduler
import es.leitner.core.ReminderTime

/** Activar el aviso diario y elegir la hora. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderSection(prefs: AppPrefs) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(prefs.reminderEnabled) }
    var hour by remember { mutableIntStateOf(prefs.reminderHour) }
    var minute by remember { mutableIntStateOf(prefs.reminderMinute) }
    var pickTime by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf<String?>(null) }

    fun apply(on: Boolean) {
        enabled = on
        prefs.reminderEnabled = on
        if (on) ReminderScheduler.schedule(context, hour, minute) else ReminderScheduler.cancel(context)
    }

    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) apply(true) else info = "Sin permiso de notificaciones no se puede avisar"
    }

    fun turnOn() {
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (needsPermission) askPermission.launch(Manifest.permission.POST_NOTIFICATIONS) else apply(true)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Recordatorio diario", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Aviso si hay tarjetas pendientes (de cualquier modo).",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Switch(checked = enabled, onCheckedChange = { if (it) turnOn() else apply(false) })
        }
        if (enabled) {
            OutlinedButton(onClick = { pickTime = true }) { Text("Hora: ${ReminderTime.format(hour, minute)}") }
        }
        info?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }

    if (pickTime) {
        val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickTime = false },
            title = { Text("Hora del aviso") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    hour = state.hour
                    minute = state.minute
                    prefs.reminderHour = hour
                    prefs.reminderMinute = minute
                    pickTime = false
                    apply(true)
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { pickTime = false }) { Text("Cancelar") } },
        )
    }
}
