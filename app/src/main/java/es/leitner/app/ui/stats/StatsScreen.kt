package es.leitner.app.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.leitner.app.R
import es.leitner.app.ui.common.modeString
import es.leitner.app.ui.common.pluralResource
import es.leitner.app.ui.common.centeredMaxWidth
import es.leitner.core.Achievement
import es.leitner.core.Mode
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: StatsViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val kids = viewModel.mode == Mode.KIDS

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(modeString(kids, R.string.my_achievements, R.string.stats)) },
                navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.back)) } },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).centeredMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(stringResource(R.string.streak), pluralResource(R.plurals.days, state.streak), Modifier.weight(1f))
                StatTile(
                    modeString(kids, R.string.today_kids, R.string.today),
                    "${state.correctToday}/${state.reviewedToday}",
                    Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(stringResource(R.string.due), "${state.dueToday}", Modifier.weight(1f))
                StatTile(stringResource(R.string.cards), "${state.totalCards}", Modifier.weight(1f))
            }

            Section(modeString(kids, R.string.per_box_kids, R.string.per_box)) {
                val max = (state.perBox.maxOrNull() ?: 0).coerceAtLeast(1)
                state.perBox.forEachIndexed { index, count ->
                    val description = stringResource(R.string.cd_box_count, index + 1, count)
                    Row(
                        Modifier.clearAndSetSemantics { contentDescription = description },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.box_n, index + 1), Modifier.width(72.dp))
                        Box(Modifier.weight(1f).height(20.dp)) {
                            Bar(fraction = count.toFloat() / max)
                        }
                        Text("$count", Modifier.width(40.dp).padding(start = 8.dp))
                    }
                }
            }

            Section(modeString(kids, R.string.stickers_kids, R.string.stickers, state.achievements.size, Achievement.entries.size)) {
                Achievement.entries.chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { a -> Sticker(a, earned = a in state.achievements, Modifier.weight(1f)) }
                        repeat(4 - row.size) { Box(Modifier.weight(1f)) }
                    }
                }
            }

            Section(stringResource(R.string.last_7_days)) {
                val max = (state.lastWeek.maxOfOrNull { it.total } ?: 0).coerceAtLeast(1)
                Row(
                    Modifier.fillMaxWidth().height(140.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    state.lastWeek.forEach { day ->
                        val dayName = LocalDate.ofEpochDay(day.day).dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
                        val description = stringResource(R.string.cd_day_activity, dayName, day.total)
                        Column(
                            Modifier.weight(1f).clearAndSetSemantics { contentDescription = description },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("${day.total}", style = MaterialTheme.typography.bodySmall)
                            Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.BottomCenter) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(day.total.toFloat() / max)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.primary),
                                )
                            }
                            Text(
                                LocalDate.ofEpochDay(day.day).dayOfWeek
                                    .getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(value, style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun Bar(fraction: Float) {
    Box(
        Modifier
            .fillMaxHeight()
            .fillMaxWidth(fraction.coerceIn(0f, 1f))
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.secondary),
    )
}

/** Una pegatina: en color si se ha ganado; si no, en gris con la pista para conseguirla. */
@Composable
private fun Sticker(achievement: Achievement, earned: Boolean, modifier: Modifier = Modifier) {
    val description = if (earned) {
        stringResource(R.string.cd_sticker_earned, achievement.title)
    } else {
        stringResource(R.string.cd_sticker_locked, achievement.hint)
    }
    Column(
        modifier.alpha(if (earned) 1f else 0.35f).clearAndSetSemantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(if (earned) achievement.emoji else "❔", fontSize = 36.sp)
        Text(
            if (earned) achievement.title else achievement.hint,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
    }
}
