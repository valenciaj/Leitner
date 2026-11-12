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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import es.leitner.app.R
import es.leitner.app.data.AppPrefs
import es.leitner.app.ui.ModeNames
import es.leitner.app.ui.common.PinDialog
import es.leitner.core.Mode
import es.leitner.core.Pin

private enum class PinStep { NONE, VERIFY_FOR_CHANGE, VERIFY_FOR_REMOVE, NEW }

/** Crear, cambiar o quitar el PIN que protege el modo niños. */
@Composable
fun PinSection(prefs: AppPrefs) {
    val hasPin by prefs.hasPin.collectAsState()
    val kidsName = stringResource(ModeNames.of(Mode.KIDS))
    var step by remember { mutableStateOf(PinStep.NONE) }
    var info by remember { mutableStateOf<String?>(null) }
    val removedText = stringResource(R.string.pin_removed)
    val savedText = stringResource(R.string.pin_saved)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.pin_title, kidsName), style = MaterialTheme.typography.titleMedium)
        Text(
            if (hasPin) {
                stringResource(R.string.pin_on, kidsName)
            } else {
                stringResource(R.string.pin_off, kidsName)
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (hasPin) {
                OutlinedButton(onClick = { step = PinStep.VERIFY_FOR_CHANGE }) { Text(stringResource(R.string.pin_change)) }
                TextButton(onClick = { step = PinStep.VERIFY_FOR_REMOVE }) {
                    Text(stringResource(R.string.pin_remove), color = MaterialTheme.colorScheme.error)
                }
            } else {
                OutlinedButton(onClick = { step = PinStep.NEW }) { Text(stringResource(R.string.pin_create)) }
            }
        }
        info?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    }

    when (step) {
        PinStep.NONE -> Unit
        PinStep.VERIFY_FOR_CHANGE -> PinDialog(
            title = stringResource(R.string.pin_current),
            check = prefs::checkPin,
            onDismiss = { step = PinStep.NONE },
            onSuccess = { step = PinStep.NEW },
        )
        PinStep.VERIFY_FOR_REMOVE -> PinDialog(
            title = stringResource(R.string.pin_current),
            check = prefs::checkPin,
            onDismiss = { step = PinStep.NONE },
            onSuccess = {
                prefs.clearPin()
                info = removedText
                step = PinStep.NONE
            },
        )
        PinStep.NEW -> NewPinDialog(
            onDismiss = { step = PinStep.NONE },
            onSave = {
                prefs.setPin(it)
                info = savedText
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
        title = { Text(stringResource(R.string.pin_new)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PinField(stringResource(R.string.pin_4_digits), first) { first = it }
                PinField(stringResource(R.string.pin_repeat), second) { second = it }
                if (mismatch) Text(stringResource(R.string.pin_mismatch), color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(
                enabled = Pin.isValidFormat(first) && first == second,
                onClick = { onSave(first) },
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
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
