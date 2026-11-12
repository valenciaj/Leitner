package es.leitner.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.leitner.app.data.AppPrefs
import es.leitner.app.data.BackupManager
import es.leitner.core.Mode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, prefs: AppPrefs, backup: BackupManager, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val modes = listOf(Mode.KIDS, Mode.NORMAL)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes de cajas") },
                navigationIcon = { TextButton(onClick = onBack) { Text("‹ Atrás") } },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = modes.indexOf(state.mode)) {
                modes.forEach { mode ->
                    Tab(
                        selected = state.mode == mode,
                        onClick = { viewModel.selectMode(mode) },
                        text = { Text(if (mode == Mode.KIDS) "Modo niños" else "Modo normal") },
                    )
                }
            }

            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "Días hasta el siguiente repaso de cada caja. Si aciertas, la tarjeta sube de caja; si fallas, vuelve a la 1.",
                    style = MaterialTheme.typography.bodyMedium,
                )

                state.intervals.forEachIndexed { index, value ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Caja ${index + 1}", Modifier.width(80.dp))
                        OutlinedTextField(
                            value = value,
                            onValueChange = { viewModel.setInterval(index, it) },
                            suffix = { Text("días") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = viewModel::addBox) { Text("+ Caja") }
                    OutlinedButton(onClick = viewModel::removeBox) { Text("− Caja") }
                    TextButton(onClick = viewModel::restoreDefaults) { Text("Por defecto") }
                }

                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (state.saved) Text("Guardado ✓", color = MaterialTheme.colorScheme.primary)

                Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) { Text("Guardar") }

                Text(
                    "Si quitas cajas, las tarjetas de las cajas eliminadas pasan a la última.",
                    style = MaterialTheme.typography.bodySmall,
                )

                HorizontalDivider()
                PinSection(prefs)

                HorizontalDivider()
                ReminderSection(prefs)

                HorizontalDivider()
                BackupSection(backup)
            }
        }
    }
}
