package es.leitner.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import es.leitner.app.ui.common.PinDialog
import es.leitner.app.ui.common.isWideScreen
import es.leitner.app.ui.deck.DeckScreen
import es.leitner.app.ui.deck.DeckViewModel
import es.leitner.app.ui.home.HomeScreen
import es.leitner.app.ui.home.HomeViewModel
import es.leitner.app.ui.mode.ModeSelectScreen
import es.leitner.app.ui.review.ReviewScreen
import es.leitner.app.ui.review.ReviewViewModel
import es.leitner.app.ui.settings.SettingsScreen
import es.leitner.app.ui.settings.SettingsViewModel
import es.leitner.app.ui.stats.StatsScreen
import es.leitner.app.ui.stats.StatsViewModel
import es.leitner.app.ui.theme.LeitnerTheme
import es.leitner.app.ui.theme.LocalThemeChoice
import es.leitner.app.widget.DueWidget
import es.leitner.core.Mode

class MainActivity : ComponentActivity() {
    override fun onStop() {
        super.onStop()
        // Al salir de la app, el widget muestra las pendientes que quedan.
        DueWidget.refresh(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as LeitnerApp
        val repository = app.repository
        val prefs = app.prefs
        // Si la app se cerró en modo niños con PIN, vuelve a abrirse en modo niños.
        val startInKids = savedInstanceState == null && prefs.lastMode == Mode.KIDS && prefs.hasPin.value

        setContent {
            val nav = rememberNavController()
            val hasPin by prefs.hasPin.collectAsState()
            LaunchedEffect(Unit) {
                if (startInKids) nav.navigate("home/${Mode.KIDS.name}")
            }
            val theme by prefs.theme.collectAsState()
            val dailyGoals by prefs.dailyGoals.collectAsState()
            CompositionLocalProvider(LocalThemeChoice provides theme) {
                NavHost(navController = nav, startDestination = "mode") {
                    composable("mode") {
                        LaunchedEffect(Unit) { prefs.lastMode = null }
                        LeitnerTheme {
                            ModeSelectScreen(
                                onModeSelected = { mode -> nav.navigate("home/${mode.name}") },
                                onOpenSettings = { nav.navigate("settings") },
                            )
                        }
                    }
                    composable(
                        "home/{mode}",
                        arguments = listOf(navArgument("mode") { type = NavType.StringType }),
                    ) { entry ->
                        val mode = Mode.valueOf(entry.arguments?.getString("mode") ?: Mode.NORMAL.name)
                        val vm: HomeViewModel = viewModel(key = mode.name, factory = HomeViewModel.Factory(repository, mode))
                        // Salir del modo niños pide el PIN (si hay uno configurado).
                        LaunchedEffect(mode) { prefs.lastMode = mode }
                        val locked = mode == Mode.KIDS && hasPin
                        var askPin by remember { mutableStateOf(false) }
                        val leave = { nav.popBackStack("mode", inclusive = false); Unit }
                        BackHandler(enabled = locked) { askPin = true }
                        LeitnerTheme(mode) {
                            if (askPin) {
                                PinDialog(
                                    title = stringResource(R.string.pin_to_leave),
                                    check = prefs::checkPin,
                                    onDismiss = { askPin = false },
                                    onSuccess = { askPin = false; leave() },
                                )
                            }
                            // En tablet, los mazos a la izquierda y el mazo abierto a la derecha.
                            val wide = isWideScreen()
                            var selectedDeck by rememberSaveable { mutableStateOf<Long?>(null) }
                            // Atrás cierra primero el mazo abierto a la derecha.
                            BackHandler(enabled = wide && selectedDeck != null) { selectedDeck = null }
                            val home: @Composable (Modifier) -> Unit = { modifier ->
                                Box(modifier) {
                                    HomeScreen(
                                        viewModel = vm,
                                        deckShare = app.deckShare,
                                        dailyGoal = dailyGoals[mode] ?: 0,
                                        onChangeMode = { if (locked) askPin = true else leave() },
                                        onOpenDeck = { deckId ->
                                            if (wide) selectedDeck = deckId else nav.navigate("deck/${mode.name}/$deckId")
                                        },
                                        onReview = { deckId -> nav.navigate("review/${mode.name}/$deckId") },
                                        onOpenStats = { nav.navigate("stats/${mode.name}") },
                                        selectedDeckId = if (wide) selectedDeck else null,
                                    )
                                }
                            }
                            if (!wide) {
                                home(Modifier.fillMaxSize())
                            } else {
                                Row(Modifier.fillMaxSize()) {
                                    home(Modifier.weight(0.4f).fillMaxHeight())
                                    VerticalDivider()
                                    Box(Modifier.weight(0.6f).fillMaxHeight()) {
                                        val deckId = selectedDeck
                                        if (deckId == null) {
                                            Surface(Modifier.fillMaxSize()) {
                                                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                                                    Text(
                                                        stringResource(R.string.pick_deck),
                                                        style = MaterialTheme.typography.titleMedium,
                                                        textAlign = TextAlign.Center,
                                                    )
                                                }
                                            }
                                        } else {
                                            val deckVm: DeckViewModel = viewModel(
                                                key = "deck-$deckId",
                                                factory = DeckViewModel.Factory(repository, mode, deckId, resources),
                                            )
                                            DeckScreen(
                                                viewModel = deckVm,
                                                media = app.media,
                                                deckShare = app.deckShare,
                                                pinCheck = if (hasPin) prefs::checkPin else null,
                                                onBack = { selectedDeck = null },
                                                onPractice = { tag -> nav.navigate(practiceRoute(mode, deckId, tag)) },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    composable(
                        "deck/{mode}/{deckId}",
                        arguments = listOf(
                            navArgument("mode") { type = NavType.StringType },
                            navArgument("deckId") { type = NavType.LongType },
                        ),
                    ) { entry ->
                        val mode = Mode.valueOf(entry.arguments?.getString("mode") ?: Mode.NORMAL.name)
                        val deckId = entry.arguments?.getLong("deckId") ?: 0L
                        val vm: DeckViewModel = viewModel(factory = DeckViewModel.Factory(repository, mode, deckId, resources))
                        LeitnerTheme(mode) {
                            DeckScreen(
                                viewModel = vm,
                                media = app.media,
                                deckShare = app.deckShare,
                                pinCheck = if (hasPin) prefs::checkPin else null,
                                onBack = { nav.popBackStack() },
                                onPractice = { tag -> nav.navigate(practiceRoute(mode, deckId, tag)) },
                            )
                        }
                    }
                    composable(
                        "practice/{mode}/{deckId}?tag={tag}",
                        arguments = listOf(
                            navArgument("mode") { type = NavType.StringType },
                            navArgument("deckId") { type = NavType.LongType },
                            navArgument("tag") { type = NavType.StringType; nullable = true; defaultValue = null },
                        ),
                    ) { entry ->
                        val mode = Mode.valueOf(entry.arguments?.getString("mode") ?: Mode.NORMAL.name)
                        val deckId = entry.arguments?.getLong("deckId") ?: 0L
                        val tag = entry.arguments?.getString("tag")
                        val vm: ReviewViewModel = viewModel(
                            factory = ReviewViewModel.Factory(repository, mode, deckId, prefs, practice = true, practiceTag = tag),
                        )
                        LeitnerTheme(mode) {
                            ReviewScreen(viewModel = vm, onClose = { nav.popBackStack() })
                        }
                    }
                    composable(
                        "review/{mode}/{deckId}",
                        arguments = listOf(
                            navArgument("mode") { type = NavType.StringType },
                            navArgument("deckId") { type = NavType.LongType },
                        ),
                    ) { entry ->
                        val mode = Mode.valueOf(entry.arguments?.getString("mode") ?: Mode.NORMAL.name)
                        val deckId = entry.arguments?.getLong("deckId") ?: 0L
                        val vm: ReviewViewModel = viewModel(factory = ReviewViewModel.Factory(repository, mode, deckId, prefs))
                        LeitnerTheme(mode) {
                            ReviewScreen(viewModel = vm, onClose = { nav.popBackStack() })
                        }
                    }
                    composable(
                        "stats/{mode}",
                        arguments = listOf(navArgument("mode") { type = NavType.StringType }),
                    ) { entry ->
                        val mode = Mode.valueOf(entry.arguments?.getString("mode") ?: Mode.NORMAL.name)
                        val vm: StatsViewModel = viewModel(key = "stats-${mode.name}", factory = StatsViewModel.Factory(repository, mode))
                        LeitnerTheme(mode) {
                            StatsScreen(viewModel = vm, onBack = { nav.popBackStack() })
                        }
                    }
                    composable("settings") {
                        val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory(repository))
                        LeitnerTheme {
                            SettingsScreen(viewModel = vm, prefs = prefs, backup = app.backup, onBack = { nav.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}

/** Ruta de la práctica de un mazo, opcionalmente solo con las tarjetas de una etiqueta. */
private fun practiceRoute(mode: Mode, deckId: Long, tag: String?): String =
    "practice/${mode.name}/$deckId" + (tag?.let { "?tag=${Uri.encode(it)}" } ?: "")
