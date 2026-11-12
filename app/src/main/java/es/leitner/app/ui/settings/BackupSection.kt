package es.leitner.app.ui.settings

import android.net.Uri
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.leitner.app.R
import es.leitner.app.data.BackupManager
import es.leitner.app.ui.common.ConfirmDialog
import java.time.LocalDate
import kotlinx.coroutines.launch

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
                onSuccess = { context.getString(R.string.backup_saved, it.decks, it.cards) },
                onFailure = { context.getString(R.string.backup_save_failed) },
            )
            busy = false
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pendingImport = uri
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.backup_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.backup_help),
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(enabled = !busy, onClick = { exportLauncher.launch("leitner-copia-${LocalDate.now()}.zip") }) {
                Text(stringResource(R.string.backup_export))
            }
            OutlinedButton(
                enabled = !busy,
                onClick = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream")) },
            ) { Text(stringResource(R.string.backup_import)) }
        }
        if (busy) Text(stringResource(R.string.working))
        info?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    }

    pendingImport?.let { uri ->
        ConfirmDialog(
            title = stringResource(R.string.backup_import_question),
            message = stringResource(R.string.backup_import_message),
            confirmText = stringResource(R.string.backup_import),
            onDismiss = { pendingImport = null },
            onConfirm = {
                pendingImport = null
                busy = true
                scope.launch {
                    info = runCatching {
                        context.contentResolver.openInputStream(uri)!!.use { backup.import(it) }
                    }.fold(
                        onSuccess = { context.getString(R.string.backup_imported, it.decks, it.cards) },
                        onFailure = { e ->
                            context.getString(
                                when ((e as? BackupManager.InvalidBackupException)?.reason) {
                                    BackupManager.Reason.NOT_A_BACKUP -> R.string.backup_not_a_backup
                                    BackupManager.Reason.DAMAGED -> R.string.backup_damaged
                                    BackupManager.Reason.UNSUPPORTED_VERSION -> R.string.backup_unsupported
                                    null -> R.string.backup_import_failed
                                },
                            )
                        },
                    )
                    busy = false
                }
            },
        )
    }
}
