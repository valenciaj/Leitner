package es.leitner.app.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.leitner.app.R
import es.leitner.app.data.DeckShare
import es.leitner.app.data.DeckSummary
import es.leitner.app.data.LeitnerRepository
import es.leitner.app.ui.common.ActionIcon
import es.leitner.app.ui.common.MoreButton
import es.leitner.app.ui.common.TextInputDialog
import es.leitner.app.ui.common.modeString
import es.leitner.app.ui.common.pluralResource
import es.leitner.app.ui.theme.LeitnerIcons
import es.leitner.core.Mode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    deckShare: DeckShare,
    /** Tarjetas que repasar al día; 0 si no hay objetivo. */
    dailyGoal: Int,
    onChangeMode: () -> Unit,
    onOpenDeck: (Long) -> Unit,
    onReview: (Long) -> Unit,
    onOpenStats: () -> Unit,
    /** Mazo abierto en el panel derecho (solo en tablet), para resaltarlo. */
    selectedDeckId: Long? = null,
) {
    val decks by viewModel.decks.collectAsStateWithLifecycle()
    val reviewedToday by viewModel.reviewedToday.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    val kids = viewModel.mode == Mode.KIDS
    var menuOpen by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val importDeck = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val text = runCatching {
                    context.contentResolver.openInputStream(uri)!!.use { deckShare.import(it, viewModel.mode) }
                }.fold(
                    onSuccess = { context.resources.getQuantityString(R.plurals.msg_deck_imported, it.cards, it.name, it.cards) },
                    onFailure = { e ->
                        context.getString(
                            if (e is DeckShare.InvalidDeckFileException) R.string.msg_not_a_deck else R.string.msg_read_failed,
                        )
                    },
                )
                snackbar.showSnackbar(text)
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        modeString(kids, R.string.decks_title_kids, R.string.decks_title),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                actions = {
                    ActionIcon(if (kids) LeitnerIcons.Trophy else LeitnerIcons.Stats, stringResource(R.string.stats), onOpenStats)
                    ActionIcon(LeitnerIcons.SwapMode, stringResource(R.string.change_mode), onChangeMode)
                    Box {
                        MoreButton { menuOpen = true }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.import_deck)) },
                                onClick = { menuOpen = false; importDeck.launch(arrayOf("*/*")) },
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            // Sin la variante icon/text: esa anima el texto y, con animaciones
            // desactivadas, lo deja fuera de la pantalla (y de TalkBack).
            ExtendedFloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.new_deck))
            }
        },
    ) { padding ->
        if (decks.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    modeString(kids, R.string.no_decks_kids, R.string.no_decks),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            val boxCount by viewModel.boxCount.collectAsStateWithLifecycle()
            LazyVerticalGrid(
                columns = if (kids) GridCells.Adaptive(150.dp) else GridCells.Adaptive(340.dp),
                modifier = Modifier.fillMaxSize().padding(padding),
                // Hueco abajo para que el botón flotante no tape el último mazo.
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(if (kids) 14.dp else 10.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (dailyGoal > 0) {
                    item(key = "goal", span = { GridItemSpan(maxLineSpan) }) { GoalCard(reviewedToday, dailyGoal) }
                }
                val totalDue = decks.sumOf { it.dueCards }
                // Con varios mazos pendientes, un botón para repasarlos todos de una vez.
                if (decks.count { it.dueCards > 0 } > 1) {
                    item(key = "all", span = { GridItemSpan(maxLineSpan) }) {
                        ReviewAllButton(kids, totalDue) { onReview(LeitnerRepository.ALL_DECKS) }
                    }
                }
                itemsIndexed(decks, key = { _, d -> d.id }) { index, deck ->
                    val selected = deck.id == selectedDeckId
                    if (kids) {
                        KidsDeckTile(deck, index, selected, onClick = { onOpenDeck(deck.id) }, onReview = { onReview(deck.id) })
                    } else {
                        DeckRow(deck, index, boxCount, selected, onClick = { onOpenDeck(deck.id) }, onReview = { onReview(deck.id) })
                    }
                }
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

/** Colores de cada mazo (avatar o tarjeta), alternando entre los del tema. */
@Composable
private fun deckColors(index: Int): Pair<Color, Color> {
    val c = MaterialTheme.colorScheme
    return when (index % 3) {
        0 -> c.primaryContainer to c.onPrimaryContainer
        1 -> c.secondaryContainer to c.onSecondaryContainer
        else -> c.tertiaryContainer to c.onTertiaryContainer
    }
}

@Composable
private fun ReviewAllButton(kids: Boolean, totalDue: Int, onClick: () -> Unit) {
    if (kids) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(80.dp),
            shape = MaterialTheme.shapes.extraLarge,
        ) { Text(stringResource(R.string.review_all_kids, totalDue)) }
    } else {
        FilledTonalButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.large,
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.review_all, totalDue))
        }
    }
}

@Composable
private fun DeckRow(
    deck: DeckSummary,
    index: Int,
    boxCount: Int,
    selected: Boolean,
    onClick: () -> Unit,
    onReview: () -> Unit,
) {
    val (avatarBg, avatarFg) = deckColors(index)
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(44.dp).clip(MaterialTheme.shapes.small).background(avatarBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        deck.name.trim().take(1).uppercase(),
                        color = avatarFg,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.clearAndSetSemantics { },
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(deck.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    val upToDate = !deck.paused && deck.dueCards == 0 && deck.totalCards > 0
                    Text(
                        when {
                            deck.paused -> stringResource(R.string.deck_paused, deck.totalCards)
                            upToDate -> stringResource(R.string.deck_up_to_date, deck.totalCards)
                            else -> stringResource(R.string.deck_summary, deck.dueCards, deck.totalCards)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (deck.dueCards > 0) {
                    FilledIconButton(
                        onClick = onReview,
                        modifier = Modifier.size(48.dp),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.review_deck_cd, deck.name))
                    }
                } else if (deck.totalCards > 0 && !deck.paused) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            if (deck.totalCards > 0) BoxBar(deck.boxCounts(boxCount))
        }
    }
}

/** Barra con el reparto de tarjetas por caja: de la 1 (color fuerte) a la última (suave). */
@Composable
private fun BoxBar(counts: IntArray) {
    val strong = MaterialTheme.colorScheme.primary
    val soft = MaterialTheme.colorScheme.primaryContainer
    val parts = counts.mapIndexed { i, n -> stringResource(R.string.box_count_cd, i + 1, n) }.joinToString(", ")
    val description = stringResource(R.string.boxes_cd, parts)
    Row(
        Modifier.fillMaxWidth().height(8.dp).clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val last = (counts.size - 1).coerceAtLeast(1)
        counts.forEachIndexed { i, n ->
            if (n > 0) {
                Box(
                    Modifier.weight(n.toFloat()).fillMaxHeight().clip(CircleShape)
                        .background(lerp(strong, soft, i.toFloat() / last)),
                )
            }
        }
    }
}

@Composable
private fun KidsDeckTile(deck: DeckSummary, index: Int, selected: Boolean, onClick: () -> Unit, onReview: () -> Unit) {
    val (bg, fg) = deckColors(index + 1)
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 170.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = bg, contentColor = fg),
        border = if (selected) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(deck.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                when {
                    deck.paused -> stringResource(R.string.deck_paused, deck.totalCards)
                    deck.dueCards == 0 -> stringResource(R.string.deck_done_kids)
                    else -> stringResource(R.string.deck_summary_kids, deck.dueCards)
                },
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.weight(1f, fill = false))
            if (deck.dueCards > 0) {
                Button(onClick = onReview, shape = MaterialTheme.shapes.medium) {
                    Text(stringResource(R.string.review_kids))
                }
            }
        }
    }
}

/** Objetivo diario: anillo de progreso con las tarjetas repasadas hoy. */
@Composable
private fun GoalCard(done: Int, goal: Int) {
    val reached = done >= goal
    val progress = (done.toFloat() / goal).coerceIn(0f, 1f)
    val c = MaterialTheme.colorScheme
    Card(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = c.primary, contentColor = c.onPrimary),
    ) {
        Row(
            Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Box(Modifier.size(76.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = c.onPrimary,
                    trackColor = c.onPrimary.copy(alpha = 0.25f),
                    strokeWidth = 8.dp,
                    strokeCap = StrokeCap.Round,
                )
                Text(
                    stringResource(R.string.goal_fraction, done.coerceAtMost(goal), goal),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(R.string.goal_today),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    if (reached) stringResource(R.string.goal_reached, done)
                    else pluralResource(R.plurals.goal_left, goal - done),
                    style = MaterialTheme.typography.titleLarge,
                )
                if (!reached) {
                    Text(
                        stringResource(R.string.goal_percent, (progress * 100).toInt()),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}
