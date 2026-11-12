package es.leitner.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import es.leitner.app.data.AppPrefs
import es.leitner.app.ui.common.PinDialog
import es.leitner.core.Pin

private enum class PinStep { NONE, VERIFY_FOR_CHANGE, VERIFY_FOR_REMOVE, NEW }

/** Crear, cambiar o quitar el PIN que protege el modo niños. */
@Composable
fun PinSection(prefs: AppPrefs) {
    val hasPin by prefs.hasPin.collectAsState()
    var step by remember { mutableStateOf(PinStep.NONE) }
    var info by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("PIN del modo niños", style = MaterialTheme.typography.titleMedium)
        Text(
            if (hasPin) {
                "Activado: hace falta para salir del modo niños y para borrar mazos en él."
            } else {
                "Sin PIN, cualquiera puede salir del modo niños."
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (hasPin) {
                OutlinedButton(onClick = { step = PinStep.VERIFY_FOR_CHANGE }) { Text("Cambiar PIN") }
                TextButton(onClick = { step = PinStep.VERIFY_FOR_REMOVE }) {
                    Text("Quitar PIN", color = MaterialTheme.colorScheme.error)
                }
            } else {
                OutlinedButton(onClick = { step = PinStep.NEW }) { Text("Crear PIN") }
            }
        }
        info?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    }

    when (step) {
        PinStep.NONE -> Unit
        PinStep.VERIFY_FOR_CHANGE -> PinDialog(
            title = "PIN actual",
            check = prefs::checkPin,
            onDismiss = { step = PinStep.NONE },
            onSuccess = { step = PinStep.NEW },
        )
        PinStep.VERIFY_FOR_REMOVE -> PinDialog(
            title = "PIN actual",
            check = prefs::checkPin,
            onDismiss = { step = PinStep.NONE },
            onSuccess = {
                prefs.clearPin()
                info = "PIN quitado"
                step = PinStep.NONE
            },
        )
        PinStep.NEW -> NewPinDialog(
            onDismiss = { step = PinStep.NONE },
            onSave = {
                prefs.setPin(it)
                info = "PIN guardado ✓"
                step = PinStep.NONE
            },
        )
    }
}

/** Pide el PIN nuevo dos veces. */
@Composable
private fun NewPinDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    val mismatch = second.length == Pin.LENGTH && first != second

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PinField("PIN (4 números)", first) { first = it }
                PinField("Repite el PIN", second) { second = it }
                if (mismatch) Text("No coinciden", color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(
                enabled = Pin.isValidFormat(first) && first == second,
                onClick = { onSave(first) },
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun PinField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter(Char::isDigit).take(Pin.LENGTH)) },
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
    )
}
