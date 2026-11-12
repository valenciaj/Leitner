package es.leitner.app.ui.common

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import es.leitner.app.data.MediaStorage
import es.leitner.app.media.AudioRecorder
import es.leitner.app.media.rememberAudioPlayer
import kotlinx.coroutines.launch
import java.io.File

/** Contenido editable de una tarjeta. */
data class CardContent(
    val front: String = "",
    val back: String = "",
    val imagePath: String? = null,
    val audioPath: String? = null,
) {
    /** La pregunta puede ser texto, imagen o audio; la respuesta debe tener texto. */
    val isValid: Boolean
        get() = (front.isNotBlank() || imagePath != null || audioPath != null) && back.isNotBlank()
}

/**
 * Diálogo para crear o editar una tarjeta, con imagen y audio opcionales.
 * Si [onDelete] no es null, muestra el botón Borrar.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CardEditDialog(
    title: String,
    media: MediaStorage,
    initial: CardContent = CardContent(),
    onDismiss: () -> Unit,
    onSave: (CardContent) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val player = rememberAudioPlayer()
    val recorder = remember { AudioRecorder(context) }

    var front by remember { mutableStateOf(initial.front) }
    var back by remember { mutableStateOf(initial.back) }
    var imagePath by remember { mutableStateOf(initial.imagePath) }
    var audioPath by remember { mutableStateOf(initial.audioPath) }
    var recordingFile by remember { mutableStateOf<File?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    // Ficheros creados en este diálogo: si no se usan al final, se borran.
    val created = remember { mutableListOf<String>() }
    fun cleanup(keep: Set<String?>) {
        created.filter { it !in keep }.forEach(media::delete)
        created.clear()
    }

    DisposableEffect(Unit) {
        onDispose {
            if (recorder.isRecording) {
                recorder.stop()
                recordingFile?.delete()
            }
        }
    }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val path = media.importImage(uri)
                if (path != null) {
                    created += path
                    imagePath = path
                } else {
                    message = "No se pudo cargar la imagen"
                }
            }
        }
    }

    fun startRecording() {
        player.stop()
        val file = media.newAudioFile()
        runCatching { recorder.start(file) }
            .onSuccess { recordingFile = file; message = null }
            .onFailure { file.delete(); message = "No se pudo grabar" }
    }

    fun stopRecording() {
        val file = recordingFile ?: return
        recordingFile = null
        if (recorder.stop()) {
            created += file.absolutePath
            audioPath = file.absolutePath
        } else {
            file.delete()
            message = "Grabación demasiado corta"
        }
    }

    val askMic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRecording() else message = "Sin permiso de micrófono no se puede grabar"
    }

    if (confirmDelete && onDelete != null) {
        ConfirmDialog(
            title = "¿Borrar tarjeta?",
            message = "No se puede deshacer.",
            confirmText = "Borrar",
            onDismiss = { confirmDelete = false },
            onConfirm = { cleanup(emptySet()); onDelete() },
        )
        return
    }

    val content = CardContent(front.trim(), back.trim(), imagePath, audioPath)
    val dismiss = {
        player.stop()
        if (recorder.isRecording) stopRecording()
        cleanup(setOf(initial.imagePath, initial.audioPath))
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(title) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = front,
                    onValueChange = { front = it },
                    label = { Text("Pregunta") },
                    modifier = Modifier.fillMaxWidth(),
                )

                imagePath?.let {
                    CardImage(it, Modifier.fillMaxWidth().heightIn(max = 180.dp))
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) { Text(if (imagePath == null) "📷 Imagen" else "📷 Cambiar") }
                    if (imagePath != null) {
                        TextButton(onClick = { imagePath = null }) { Text("Quitar imagen") }
                    }
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (recordingFile != null) {
                        OutlinedButton(onClick = ::stopRecording) {
                            Text("⏹ Parar", color = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        OutlinedButton(onClick = {
                            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                                PackageManager.PERMISSION_GRANTED
                            if (granted) startRecording() else askMic.launch(Manifest.permission.RECORD_AUDIO)
                        }) { Text(if (audioPath == null) "🎤 Grabar" else "🎤 Regrabar") }
                    }
                    audioPath?.let { path ->
                        if (recordingFile == null) {
                            OutlinedButton(onClick = { if (player.isPlaying) player.stop() else player.play(path) }) {
                                Text(if (player.isPlaying) "⏹" else "🔊 Escuchar")
                            }
                            TextButton(onClick = { player.stop(); audioPath = null }) { Text("Quitar audio") }
                        }
                    }
                }
                if (recordingFile != null) Text("Grabando…", color = MaterialTheme.colorScheme.error)
                message?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                OutlinedTextField(
                    value = back,
                    onValueChange = { back = it },
                    label = { Text("Respuesta") },
                    modifier = Modifier.fillMaxWidth(),
                )

                if (onDelete != null) {
                    TextButton(onClick = { confirmDelete = true }) {
                        Text("Borrar tarjeta", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    player.stop()
                    cleanup(setOf(content.imagePath, content.audioPath))
                    onSave(content)
                },
                enabled = content.isValid && recordingFile == null,
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = dismiss) { Text("Cancelar") } },
    )
}
