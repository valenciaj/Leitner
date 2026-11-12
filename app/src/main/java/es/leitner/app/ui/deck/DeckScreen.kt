package es.leitner.app.ui.deck

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.leitner.app.data.CardEntity
import es.leitner.app.data.MediaStorage
import es.leitner.app.ui.common.CardContent
import es.leitner.app.ui.common.CardEditDialog
import es.leitner.app.ui.common.CardImage
import es.leitner.app.ui.common.ConfirmDialog
import es.leitner.app.ui.common.TextInputDialog
import es.leitner.core.Mode

/** Qué diálogo está abierto. */
private sealed interface DeckDialog {
    data object NewCard : DeckDialog
    data class EditCard(val card: CardEntity) : DeckDialog
    data object Rename : DeckDialog
    data object DeleteDeck : DeckDialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckScreen(viewModel: DeckViewModel, media: MediaStorage, onBack: () -> Unit) {
    val deck by viewModel.deck.collectAsStateWithLifecycle()
    val cards by viewModel.cards.collectAsStateWithLifecycle()
    val config by viewModel.config.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<DeckDialog?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    val kids = viewModel.mode == Mode.KIDS

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(deck?.name ?: "") },
                navigationIcon = { TextButton(onClick = onBack) { Text("‹ Atrás") } },
                actions = {
                    Box {
                        TextButton(onClick = { menuOpen = true }) { Text("⋮") }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Renombrar mazo") },
                                onClick = { menuOpen = false; dialog = DeckDialog.Rename },
                            )
                            // En modo niños no se pueden borrar mazos (más adelante, con PIN).
                            if (!kids) {
                                DropdownMenuItem(
                                    text = { Text("Borrar mazo", color = MaterialTheme.colorScheme.error) },
                                    onClick = { menuOpen = false; dialog = DeckDialog.DeleteDeck },
                                )
                            }
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { dialog = DeckDialog.NewCard }) {
                Text(if (kids) "➕ Nueva tarjeta" else "Nueva tarjeta")
            }
        },
    ) { padding ->
        if (cards.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    if (kids) "¡Añade tu primera tarjeta! ✏️" else "Este mazo no tiene tarjetas",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        "${cards.size} tarjetas · ${config.boxCount} cajas",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                items(cards, key = { it.id }) { card ->
                    CardRow(card, kids, onClick = { dialog = DeckDialog.EditCard(card) })
                }
            }
        }
    }

    when (val d = dialog) {
        null -> Unit
        DeckDialog.NewCard -> CardEditDialog(
            title = "Nueva tarjeta",
            media = media,
            onDismiss = { dialog = null },
            onSave = { viewModel.addCard(it); dialog = null },
        )
        is DeckDialog.EditCard -> CardEditDialog(
            title = "Editar tarjeta",
            media = media,
            initial = CardContent(d.card.front, d.card.back, d.card.imageUri, d.card.audioPath),
            onDismiss = { dialog = null },
            onSave = { viewModel.updateCard(d.card.id, it); dialog = null },
            onDelete = { viewModel.deleteCard(d.card.id); dialog = null },
        )
        DeckDialog.Rename -> TextInputDialog(
            title = "Renombrar mazo",
            initial = deck?.name.orEmpty(),
            onDismiss = { dialog = null },
            onConfirm = { viewModel.rename(it); dialog = null },
        )
        DeckDialog.DeleteDeck -> ConfirmDialog(
            title = "¿Borrar el mazo?",
            message = "Se borrarán también sus ${cards.size} tarjetas. No se puede deshacer.",
            confirmText = "Borrar",
            onDismiss = { dialog = null },
            onConfirm = { dialog = null; viewModel.deleteDeck(onDone = onBack) },
        )
    }
}

@Composable
private fun CardRow(card: CardEntity, kids: Boolean, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            Modifier.padding(if (kids) 20.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            card.imageUri?.let {
                CardImage(
                    it,
                    Modifier.size(if (kids) 64.dp else 48.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                val front = card.front.ifBlank { if (card.audioPath != null) "🔊" else "🖼" }
                Text(if (card.audioPath != null && card.front.isNotBlank()) "$front 🔊" else front, style = MaterialTheme.typography.titleMedium)
                Text(card.back, style = MaterialTheme.typography.bodyLarge)
            }
            AssistChip(onClick = onClick, label = { Text("Caja ${card.box}") })
        }
    }
}
