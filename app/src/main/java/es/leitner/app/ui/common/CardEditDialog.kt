package es.leitner.app.ui.common

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import es.leitner.app.data.CardContent
import es.leitner.app.data.MediaStorage
import es.leitner.app.media.AudioPlayer
import es.leitner.app.media.AudioRecorder
import es.leitner.app.media.rememberAudioPlayer
import kotlinx.coroutines.launch
import java.io.File

/** Lado de la tarjeta. */
private enum class Side { FRONT, BACK }

/**
 * Diálogo para crear o editar una tarjeta, con imagen y audio opcionales en la pregunta y en la respuesta.
 * Si [onDelete] no es null, muestra el botón Borrar.
 * Si [offerReverse] es true, ofrece crear también la tarjeta al revés; su valor llega en [onSave].
 */
@Composable
fun CardEditDialog(
    title: String,
    media: MediaStorage,
    initial: CardContent = CardContent(),
    offerReverse: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (content: CardContent, alsoReversed: Boolean) -> Unit,
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
    var backImagePath by remember { mutableStateOf(initial.backImagePath) }
    var backAudioPath by remember { mutableStateOf(initial.backAudioPath) }
    var alsoReversed by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf<Pair<Side, File>?>(null) }
    var pickingFor by remember { mutableStateOf(Side.FRONT) }
    var recordFor by remember { mutableStateOf(Side.FRONT) }
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
                recording?.second?.delete()
            }
        }
    }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val side = pickingFor
            scope.launch {
                val path = media.importImage(uri)
                if (path != null) {
                    created += path
                    if (side == Side.FRONT) imagePath = path else backImagePath = path
                } else {
                    message = "No se pudo cargar la imagen"
                }
            }
        }
    }

    fun startRecording(side: Side) {
        player.stop()
        val file = media.newAudioFile()
        runCatching { recorder.start(file) }
            .onSuccess { recording = side to file; message = null }
            .onFailure { file.delete(); message = "No se pudo grabar" }
    }

    fun stopRecording() {
        val (side, file) = recording ?: return
        recording = null
        if (recorder.stop()) {
            created += file.absolutePath
            if (side == Side.FRONT) audioPath = file.absolutePath else backAudioPath = file.absolutePath
        } else {
            file.delete()
            message = "Grabación demasiado corta"
        }
    }

    val askMic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRecording(recordFor) else message = "Sin permiso de micrófono no se puede grabar"
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

    val content = CardContent(front.trim(), back.trim(), imagePath, audioPath, backImagePath, backAudioPath)
    val dismiss = {
        player.stop()
        if (recorder.isRecording) stopRecording()
        cleanup(initial.files)
        onDismiss()
    }

    @Composable
    fun Media(side: Side) {
        MediaButtons(
            imagePath = if (side == Side.FRONT) imagePath else backImagePath,
            audioPath = if (side == Side.FRONT) audioPath else backAudioPath,
            isRecording = recording?.first == side,
            recordingOther = recording != null && recording?.first != side,
            player = player,
            onPickImage = {
                pickingFor = side
                pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onRemoveImage = { if (side == Side.FRONT) imagePath = null else backImagePath = null },
            onRecord = {
                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
                if (granted) {
                    startRecording(side)
                } else {
                    recordFor = side
                    askMic.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            onStopRecording = ::stopRecording,
            onRemoveAudio = { player.stop(); if (side == Side.FRONT) audioPath = null else backAudioPath = null },
        )
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
                Media(Side.FRONT)

                OutlinedTextField(
                    value = back,
                    onValueChange = { back = it },
                    label = { Text("Respuesta") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Media(Side.BACK)

                if (recording != null) Text("Grabando…", color = MaterialTheme.colorScheme.error)
                message?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                if (offerReverse) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = alsoReversed, onCheckedChange = { alsoReversed = it })
                        Text("Crear también al revés")
                    }
                }

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
                    cleanup(content.files)
                    onSave(content, offerReverse && alsoReversed)
                },
                enabled = content.isValid && recording == null,
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = dismiss) { Text("Cancelar") } },
    )
}

/** Imagen y audio de un lado de la tarjeta. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MediaButtons(
    imagePath: String?,
    audioPath: String?,
    isRecording: Boolean,
    recordingOther: Boolean,
    player: AudioPlayer,
    onPickImage: () -> Unit,
    onRemoveImage: () -> Unit,
    onRecord: () -> Unit,
    onStopRecording: () -> Unit,
    onRemoveAudio: () -> Unit,
) {
    imagePath?.let {
        CardImage(it, Modifier.fillMaxWidth().heightIn(max = 180.dp))
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onPickImage) { Text(if (imagePath == null) "📷 Imagen" else "📷 Cambiar") }
        if (imagePath != null) {
            TextButton(onClick = onRemoveImage) { Text("Quitar imagen") }
        }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (isRecording) {
            OutlinedButton(onClick = onStopRecording) {
                Text("⏹ Parar", color = MaterialTheme.colorScheme.error)
            }
        } else {
            OutlinedButton(onClick = onRecord, enabled = !recordingOther) {
                Text(if (audioPath == null) "🎤 Grabar" else "🎤 Regrabar")
            }
            audioPath?.let { path ->
                OutlinedButton(onClick = { if (player.isPlaying) player.stop() else player.play(path) }) {
                    Text(if (player.isPlaying) "⏹" else "🔊 Escuchar")
                }
                TextButton(onClick = onRemoveAudio) { Text("Quitar audio") }
            }
        }
    }
}
