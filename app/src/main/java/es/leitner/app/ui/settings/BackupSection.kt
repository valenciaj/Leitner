package es.leitner.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.net.Uri
import es.leitner.app.data.BackupManager
import es.leitner.app.ui.common.ConfirmDialog
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Exportar e importar todos los datos (mazos, tarjetas, imágenes, audio y estadísticas). */
@Composable
fun BackupSection(backup: BackupManager) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf<String?>(null) }
    var pendingImport by remember { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            info = runCatching {
                context.contentResolver.openOutputStream(uri)!!.use { backup.export(it) }
            }.fold(
                onSuccess = { "Copia guardada: ${it.decks} mazos, ${it.cards} tarjetas ✓" },
                onFailure = { "No se pudo guardar la copia" },
            )
            busy = false
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pendingImport = uri
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Copia de seguridad", style = MaterialTheme.typography.titleMedium)
        Text(
            "Guarda en un fichero todos los mazos, tarjetas, imágenes, audios y estadísticas de los dos modos.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(enabled = !busy, onClick = { exportLauncher.launch("leitner-copia-${LocalDate.now()}.zip") }) {
                Text("Exportar")
            }
            OutlinedButton(
                enabled = !busy,
                onClick = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream")) },
            ) { Text("Importar") }
        }
        if (busy) Text("Trabajando…")
        info?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    }

    pendingImport?.let { uri ->
        ConfirmDialog(
            title = "¿Importar la copia?",
            message = "Se borrarán todos los datos actuales y se sustituirán por los de la copia.",
            confirmText = "Importar",
            onDismiss = { pendingImport = null },
            onConfirm = {
                pendingImport = null
                busy = true
                scope.launch {
                    info = runCatching {
                        context.contentResolver.openInputStream(uri)!!.use { backup.import(it) }
                    }.fold(
                        onSuccess = { "Copia importada: ${it.decks} mazos, ${it.cards} tarjetas ✓" },
                        onFailure = { (it as? BackupManager.InvalidBackupException)?.message ?: "No se pudo importar la copia" },
                    )
                    busy = false
                }
            },
        )
    }
}
