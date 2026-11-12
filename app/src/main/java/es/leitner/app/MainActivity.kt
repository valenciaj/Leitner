package es.leitner.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import es.leitner.app.ui.home.HomeScreen
import es.leitner.app.ui.home.HomeViewModel
import es.leitner.app.ui.mode.ModeSelectScreen
import es.leitner.app.ui.theme.LeitnerTheme
import es.leitner.core.Mode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as LeitnerApp).repository

        setContent {
            val nav = rememberNavController()
            NavHost(navController = nav, startDestination = "mode") {
                composable("mode") {
                    LeitnerTheme {
                        ModeSelectScreen(onModeSelected = { mode -> nav.navigate("home/${mode.name}") })
                    }
                }
                composable(
                    "home/{mode}",
                    arguments = listOf(navArgument("mode") { type = NavType.StringType }),
                ) { entry ->
                    val mode = Mode.valueOf(entry.arguments?.getString("mode") ?: Mode.NORMAL.name)
                    val vm: HomeViewModel = viewModel(key = mode.name, factory = HomeViewModel.Factory(repository, mode))
                    LeitnerTheme(mode) {
                        HomeScreen(viewModel = vm, onChangeMode = { nav.popBackStack("mode", inclusive = false) })
                    }
                }
            }
        }
    }
}
