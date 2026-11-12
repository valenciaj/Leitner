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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
                title = { Text(if (kids) "Mis logros" else "Estadísticas") },
                navigationIcon = { TextButton(onClick = onBack) { Text("‹ Atrás") } },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("🔥 Racha", "${state.streak} días", Modifier.weight(1f))
                StatTile(
                    if (kids) "⭐ Hoy" else "Hoy",
                    "${state.correctToday}/${state.reviewedToday}",
                    Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Pendientes", "${state.dueToday}", Modifier.weight(1f))
                StatTile("Tarjetas", "${state.totalCards}", Modifier.weight(1f))
            }

            Section(if (kids) "¿Dónde están mis tarjetas?" else "Tarjetas por caja") {
                val max = (state.perBox.maxOrNull() ?: 0).coerceAtLeast(1)
                state.perBox.forEachIndexed { index, count ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Caja ${index + 1}", Modifier.width(72.dp))
                        Box(Modifier.weight(1f).height(20.dp)) {
                            Bar(fraction = count.toFloat() / max)
                        }
                        Text("$count", Modifier.width(40.dp).padding(start = 8.dp))
                    }
                }
            }

            Section(if (kids) "Mis pegatinas (${state.achievements.size}/${Achievement.entries.size})" else "Logros (${state.achievements.size}/${Achievement.entries.size})") {
                Achievement.entries.chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { a -> Sticker(a, earned = a in state.achievements, Modifier.weight(1f)) }
                        repeat(4 - row.size) { Box(Modifier.weight(1f)) }
                    }
                }
            }

            Section("Últimos 7 días") {
                val max = (state.lastWeek.maxOfOrNull { it.total } ?: 0).coerceAtLeast(1)
                Row(
                    Modifier.fillMaxWidth().height(140.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    state.lastWeek.forEach { day ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
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
                                    .getDisplayName(TextStyle.NARROW, Locale("es")),
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
    Column(modifier.alpha(if (earned) 1f else 0.35f), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(if (earned) achievement.emoji else "❔", fontSize = 36.sp)
        Text(
            if (earned) achievement.title else achievement.hint,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
    }
}
