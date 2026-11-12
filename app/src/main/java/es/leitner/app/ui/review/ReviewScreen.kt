package es.leitner.app.ui.review

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.leitner.app.media.rememberAudioPlayer
import es.leitner.app.media.rememberSpeaker
import es.leitner.app.ui.common.CardImage
import es.leitner.core.Mode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(viewModel: ReviewViewModel, onClose: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val kids = viewModel.mode == Mode.KIDS

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (kids) "¡A jugar!" else "Repaso") },
                navigationIcon = { TextButton(onClick = onClose) { Text("✕ Salir") } },
                actions = {
                    if (state.canUndo) TextButton(onClick = viewModel::undo) { Text("↶ Deshacer") }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            when {
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.finished -> Summary(state, kids, onClose)
                else -> Question(state, kids, onReveal = viewModel::reveal, onAnswer = viewModel::answer)
            }
        }
    }
}

@Composable
private fun Question(
    state: ReviewUiState,
    kids: Boolean,
    onReveal: () -> Unit,
    onAnswer: (Boolean) -> Unit,
) {
    val card = state.card ?: return
    val player = rememberAudioPlayer()
    val speaker = rememberSpeaker()
    // En modo niños la pregunta suena sola: el audio grabado o, si no hay, la voz del móvil.
    LaunchedEffect(card.id, state.position) {
        player.stop()
        speaker.stop()
        if (kids) {
            if (card.audioPath != null) player.play(card.audioPath) else speaker.speak(card.front)
        }
    }
    // En modo niños también se lee la respuesta al darle la vuelta.
    LaunchedEffect(card.id, state.position, state.revealed) {
        if (kids && state.revealed) {
            player.stop()
            speaker.speak(card.back)
        }
    }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        LinearProgressIndicator(
            progress = { (state.position - 1).toFloat() / state.queueSize },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${state.position} / ${state.queueSize}", style = MaterialTheme.typography.bodyMedium)
            if (state.isRetry) {
                Text(
                    if (kids) "🔁 ¡Otra vez!" else "🔁 Repaso de falladas",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }

        Card(
            Modifier.fillMaxWidth().weight(1f).clickable(enabled = !state.revealed, onClick = onReveal),
        ) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                card.imageUri?.let {
                    CardImage(it, Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(max = 260.dp))
                    Spacer(Modifier.height(16.dp))
                }
                if (card.front.isNotBlank()) {
                    Text(
                        card.front,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        fontSize = if (kids) 36.sp else 28.sp,
                    )
                    if (card.audioPath == null) {
                        TextButton(onClick = { speaker.speak(card.front) }) {
                            Text("🗣 Leer", fontSize = if (kids) 22.sp else 14.sp)
                        }
                    }
                }
                card.audioPath?.let { path ->
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { if (player.isPlaying) player.stop() else player.play(path) },
                        modifier = Modifier.height(if (kids) 64.dp else 48.dp),
                    ) { Text(if (player.isPlaying) "⏹ Parar" else "🔊 Escuchar", fontSize = if (kids) 24.sp else 16.sp) }
                }
                if (state.revealed) {
                    HorizontalDivider(Modifier.padding(vertical = 24.dp))
                    Text(
                        card.back,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        fontSize = if (kids) 32.sp else 24.sp,
                    )
                    TextButton(onClick = { speaker.speak(card.back) }) {
                        Text("🗣 Leer", fontSize = if (kids) 22.sp else 14.sp)
                    }
                } else {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        if (kids) "👆 Toca para ver la respuesta" else "Toca para ver la respuesta",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        if (state.revealed) {
            val height = if (kids) 88.dp else 56.dp
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = { onAnswer(false) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                    modifier = Modifier.weight(1f).height(height),
                ) { Text(if (kids) "❌ Fallé" else "Fallé") }
                Button(
                    onClick = { onAnswer(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047)),
                    modifier = Modifier.weight(1f).height(height),
                ) { Text(if (kids) "✅ ¡Acerté!" else "Acerté") }
            }
        } else {
            Button(onClick = onReveal, modifier = Modifier.fillMaxWidth().height(if (kids) 88.dp else 56.dp)) {
                Text("Ver respuesta")
            }
        }
    }
}

@Composable
private fun Summary(state: ReviewUiState, kids: Boolean, onClose: () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state.total == 0) {
            Text(
                if (kids) "¡Hoy no hay tarjetas! 🎈" else "No hay tarjetas pendientes hoy",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
        } else if (kids) {
            Text("⭐".repeat(state.stars).ifEmpty { "💪" }, fontSize = 64.sp)
            Text(
                when (state.stars) {
                    3 -> "¡Increíble!"
                    2 -> "¡Muy bien!"
                    1 -> "¡Buen trabajo!"
                    else -> "¡Sigue practicando!"
                },
                style = MaterialTheme.typography.titleLarge,
            )
            Text("Acertaste ${state.correct} de ${state.total}", style = MaterialTheme.typography.bodyLarge)
        } else {
            Text("Repaso terminado", style = MaterialTheme.typography.titleLarge)
            Text("Aciertos: ${state.correct} · Fallos: ${state.wrong}", style = MaterialTheme.typography.bodyLarge)
        }
        Button(onClick = onClose, modifier = Modifier.height(if (kids) 72.dp else 48.dp)) { Text("Volver") }
    }
}
