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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.leitner.app.R
import es.leitner.app.media.rememberAudioPlayer
import es.leitner.app.media.rememberSpeaker
import es.leitner.app.ui.common.CardImage
import es.leitner.app.ui.common.centeredMaxWidth
import es.leitner.app.ui.common.modeString
import es.leitner.app.ui.common.pluralResource
import es.leitner.app.ui.common.titleRes
import es.leitner.core.Mode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(viewModel: ReviewViewModel, onClose: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val kids = viewModel.mode == Mode.KIDS

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (viewModel.practice) stringResource(R.string.practice_title)
                        else modeString(kids, R.string.review_title_kids, R.string.review_title),
                    )
                },
                navigationIcon = { TextButton(onClick = onClose) { Text(stringResource(R.string.exit)) } },
                actions = {
                    val typeLabel = stringResource(if (state.typeAnswer) R.string.type_answer_off else R.string.type_answer_on)
                    TextButton(
                        onClick = viewModel::toggleTypeAnswer,
                        modifier = Modifier.semantics { contentDescription = typeLabel },
                    ) { Text(if (state.typeAnswer) "✍✓" else "✍") }
                    if (state.canUndo) TextButton(onClick = viewModel::undo) { Text(stringResource(R.string.undo)) }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).centeredMaxWidth().padding(16.dp)) {
            when {
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.finished -> Summary(state, kids, viewModel.practice, onClose)
                else -> Question(state, kids, onReveal = viewModel::reveal, onAnswer = viewModel::answer, onCheck = viewModel::check)
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
    onCheck: (String) -> Unit,
) {
    val card = state.card ?: return
    // Escribir la respuesta solo tiene sentido si la respuesta tiene texto.
    val typing = state.typeAnswer && card.back.isNotBlank()
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
            if (card.backAudioPath != null) player.play(card.backAudioPath) else speaker.speak(card.back)
        }
    }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        LinearProgressIndicator(
            progress = { (state.position - 1).toFloat() / state.queueSize },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.progress, state.position, state.queueSize), style = MaterialTheme.typography.bodyMedium)
            if (state.isRetry) {
                Text(
                    modeString(kids, R.string.retry_kids, R.string.retry),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }

        Card(
            Modifier.fillMaxWidth().weight(1f).clickable(
                enabled = !state.revealed,
                onClickLabel = stringResource(R.string.show_answer),
                onClick = onReveal,
            ),
        ) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                card.imageUri?.let {
                    CardImage(
                        it,
                        Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(max = 260.dp),
                        contentDescription = stringResource(R.string.cd_question_image),
                    )
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
                            Text(stringResource(R.string.read_aloud), fontSize = if (kids) 22.sp else 14.sp)
                        }
                    }
                }
                card.audioPath?.let { path ->
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { if (player.isPlaying) player.stop() else player.play(path) },
                        modifier = Modifier.height(if (kids) 64.dp else 48.dp),
                    ) { Text(stringResource(if (player.isPlaying) R.string.stop else R.string.listen), fontSize = if (kids) 24.sp else 16.sp) }
                }
                if (state.revealed) {
                    HorizontalDivider(Modifier.padding(vertical = 24.dp))
                    card.backImageUri?.let {
                        CardImage(
                            it,
                            Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(max = 220.dp),
                            contentDescription = stringResource(R.string.cd_answer_image),
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    if (card.back.isNotBlank()) {
                        Text(
                            card.back,
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center,
                            fontSize = if (kids) 32.sp else 24.sp,
                        )
                    }
                    val backAudio = card.backAudioPath
                    if (backAudio != null) {
                        OutlinedButton(
                            onClick = { if (player.isPlaying) player.stop() else player.play(backAudio) },
                            modifier = Modifier.height(if (kids) 64.dp else 48.dp),
                        ) { Text(stringResource(if (player.isPlaying) R.string.stop else R.string.listen), fontSize = if (kids) 24.sp else 16.sp) }
                    } else if (card.back.isNotBlank()) {
                        TextButton(onClick = { speaker.speak(card.back) }) {
                            Text(stringResource(R.string.read_aloud), fontSize = if (kids) 22.sp else 14.sp)
                        }
                    }
                } else {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        modeString(kids, R.string.tap_to_reveal_kids, R.string.tap_to_reveal),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        state.typedCorrect?.let { correct ->
            Text(
                if (correct) stringResource(R.string.typed_right) else stringResource(R.string.typed_wrong, state.typedText),
                style = MaterialTheme.typography.titleMedium,
                color = if (correct) Color(0xFF43A047) else MaterialTheme.colorScheme.error,
            )
        }
        if (state.revealed) {
            val height = if (kids) 88.dp else 56.dp
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = { onAnswer(false) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                    modifier = Modifier.weight(1f).height(height),
                ) { Text(modeString(kids, R.string.wrong_kids, R.string.wrong)) }
                Button(
                    onClick = { onAnswer(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047)),
                    modifier = Modifier.weight(1f).height(height),
                ) { Text(modeString(kids, R.string.right_kids, R.string.right)) }
            }
        } else if (typing) {
            var typed by rememberSaveable(card.id, state.position) { mutableStateOf("") }
            OutlinedTextField(
                value = typed,
                onValueChange = { typed = it },
                label = { Text(stringResource(R.string.your_answer)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (typed.isNotBlank()) onCheck(typed) }),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { onCheck(typed) },
                enabled = typed.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(if (kids) 88.dp else 56.dp),
            ) { Text(stringResource(R.string.check)) }
        } else {
            Button(onClick = onReveal, modifier = Modifier.fillMaxWidth().height(if (kids) 88.dp else 56.dp)) {
                Text(stringResource(R.string.show_answer))
            }
        }
    }
}

@Composable
private fun Summary(state: ReviewUiState, kids: Boolean, practice: Boolean, onClose: () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state.total == 0) {
            Text(
                modeString(kids, R.string.nothing_due_kids, R.string.nothing_due),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
        } else if (kids) {
            val starsLabel = pluralResource(R.plurals.cd_stars, state.stars)
            Text(
                "⭐".repeat(state.stars).ifEmpty { "💪" },
                fontSize = 64.sp,
                modifier = Modifier.semantics { contentDescription = starsLabel },
            )
            Text(
                stringResource(
                    when (state.stars) {
                        3 -> R.string.praise_3
                        2 -> R.string.praise_2
                        1 -> R.string.praise_1
                        else -> R.string.praise_0
                    },
                ),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(stringResource(R.string.kids_result, state.correct, state.total), style = MaterialTheme.typography.bodyLarge)
        } else {
            Text(stringResource(R.string.review_done), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.review_result, state.correct, state.wrong), style = MaterialTheme.typography.bodyLarge)
        }
        if (practice && state.total > 0) {
            Text(
                stringResource(R.string.practice_note),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
        state.newAchievements.forEach { a ->
            Card {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(a.emoji, fontSize = if (kids) 48.sp else 32.sp)
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(modeString(kids, R.string.new_sticker, R.string.new_achievement), style = MaterialTheme.typography.bodyMedium)
                        Text(stringResource(a.titleRes), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
        Button(onClick = onClose, modifier = Modifier.height(if (kids) 72.dp else 48.dp)) { Text(stringResource(R.string.back_home)) }
    }
}
