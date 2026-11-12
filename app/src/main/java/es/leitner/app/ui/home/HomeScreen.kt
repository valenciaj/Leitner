package es.leitner.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.leitner.app.data.DeckSummary
import es.leitner.app.ui.common.TextInputDialog
import es.leitner.core.Mode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel, onChangeMode: () -> Unit, onOpenDeck: (Long) -> Unit) {
    val decks by viewModel.decks.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    val kids = viewModel.mode == Mode.KIDS

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (kids) "Mis tarjetas" else "Mazos") },
                actions = { TextButton(onClick = onChangeMode) { Text("Cambiar modo") } },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showDialog = true }) {
                Text(if (kids) "➕ Nuevo mazo" else "Nuevo mazo")
            }
        },
    ) { padding ->
        if (decks.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    if (kids) "¡Crea tu primer mazo! 🎉" else "Aún no hay mazos",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(decks, key = { it.id }) { deck -> DeckRow(deck, kids, onClick = { onOpenDeck(deck.id) }) }
            }
        }
    }

    if (showDialog) {
        TextInputDialog(
            title = "Nuevo mazo",
            confirmText = "Crear",
            onDismiss = { showDialog = false },
            onConfirm = {
                viewModel.createDeck(it)
                showDialog = false
            },
        )
    }
}

@Composable
private fun DeckRow(deck: DeckSummary, kids: Boolean, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(if (kids) 24.dp else 16.dp)) {
            Text(deck.name, style = MaterialTheme.typography.titleMedium)
            Text(
                if (kids) "${deck.dueCards} para hoy ⭐" else "${deck.dueCards} pendientes · ${deck.totalCards} tarjetas",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
