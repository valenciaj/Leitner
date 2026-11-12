package es.leitner.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.leitner.app.R
import es.leitner.app.data.DeckSummary
import es.leitner.app.data.LeitnerRepository
import es.leitner.app.ui.common.TextInputDialog
import es.leitner.app.ui.common.modeString
import es.leitner.core.Mode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onChangeMode: () -> Unit,
    onOpenDeck: (Long) -> Unit,
    onReview: (Long) -> Unit,
    onOpenStats: () -> Unit,
    /** Mazo abierto en el panel derecho (solo en tablet), para resaltarlo. */
    selectedDeckId: Long? = null,
) {
    val decks by viewModel.decks.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    val kids = viewModel.mode == Mode.KIDS

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(modeString(kids, R.string.decks_title_kids, R.string.decks_title)) },
                actions = {
                    val statsLabel = stringResource(R.string.stats)
                    TextButton(onClick = onOpenStats, modifier = Modifier.semantics { contentDescription = statsLabel }) {
                        Text(modeString(kids, R.string.stats_kids, R.string.stats))
                    }
                    TextButton(onClick = onChangeMode) { Text(stringResource(R.string.change_mode)) }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showDialog = true }) {
                Text(modeString(kids, R.string.new_deck_kids, R.string.new_deck))
            }
        },
    ) { padding ->
        if (decks.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    modeString(kids, R.string.no_decks_kids, R.string.no_decks),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val totalDue = decks.sumOf { it.dueCards }
                // Con varios mazos pendientes, un botón para repasarlos todos de una vez.
                if (decks.count { it.dueCards > 0 } > 1) {
                    item(key = "all") {
                        Button(
                            onClick = { onReview(LeitnerRepository.ALL_DECKS) },
                            modifier = Modifier.fillMaxWidth().height(if (kids) 72.dp else 56.dp),
                        ) { Text(modeString(kids, R.string.review_all_kids, R.string.review_all, totalDue)) }
                    }
                }
                items(decks, key = { it.id }) { deck -> DeckRow(deck, kids, selected = deck.id == selectedDeckId, onClick = { onOpenDeck(deck.id) }, onReview = { onReview(deck.id) }) }
            }
        }
    }

    if (showDialog) {
        TextInputDialog(
            title = stringResource(R.string.new_deck),
            confirmText = stringResource(R.string.create),
            onDismiss = { showDialog = false },
            onConfirm = {
                viewModel.createDeck(it)
                showDialog = false
            },
        )
    }
}

@Composable
private fun DeckRow(deck: DeckSummary, kids: Boolean, selected: Boolean, onClick: () -> Unit, onReview: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = if (selected) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer) else CardDefaults.cardColors(),
    ) {
        Row(Modifier.padding(if (kids) 24.dp else 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(deck.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (kids) stringResource(R.string.deck_summary_kids, deck.dueCards) else stringResource(R.string.deck_summary, deck.dueCards, deck.totalCards),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            if (deck.dueCards > 0) {
                Button(onClick = onReview) { Text(modeString(kids, R.string.review_kids, R.string.review)) }
            }
        }
    }
}
