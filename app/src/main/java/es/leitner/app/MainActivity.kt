package es.leitner.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import es.leitner.app.ui.common.PinDialog
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
import es.leitner.core.Mode

class MainActivity : ComponentActivity() {
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
                                title = "PIN para salir",
                                check = prefs::checkPin,
                                onDismiss = { askPin = false },
                                onSuccess = { askPin = false; leave() },
                            )
                        }
                        HomeScreen(
                            viewModel = vm,
                            onChangeMode = { if (locked) askPin = true else leave() },
                            onOpenDeck = { deckId -> nav.navigate("deck/${mode.name}/$deckId") },
                            onReview = { deckId -> nav.navigate("review/${mode.name}/$deckId") },
                            onOpenStats = { nav.navigate("stats/${mode.name}") },
                        )
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
                    val vm: DeckViewModel = viewModel(factory = DeckViewModel.Factory(repository, mode, deckId))
                    LeitnerTheme(mode) {
                        DeckScreen(
                            viewModel = vm,
                            media = app.media,
                            pinCheck = if (hasPin) prefs::checkPin else null,
                            onBack = { nav.popBackStack() },
                        )
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
                    val vm: ReviewViewModel = viewModel(factory = ReviewViewModel.Factory(repository, mode, deckId))
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
                        SettingsScreen(viewModel = vm, prefs = prefs, onBack = { nav.popBackStack() })
                    }
                }
            }
        }
    }
}
