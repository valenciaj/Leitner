package es.leitner.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import es.leitner.app.R
import es.leitner.app.data.AppPrefs
import es.leitner.app.data.ThemeChoice

/** Elegir tema: el del móvil, claro u oscuro. */
@Composable
fun ThemeSection(prefs: AppPrefs) {
    val current by prefs.theme.collectAsState()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.selectableGroup()) {
        Text(stringResource(R.string.theme_title), style = MaterialTheme.typography.titleMedium)
        ThemeChoice.entries.forEach { choice ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(selected = choice == current, role = Role.RadioButton, onClick = { prefs.setTheme(choice) }),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = choice == current, onClick = null)
                Text(
                    stringResource(
                        when (choice) {
                            ThemeChoice.SYSTEM -> R.string.theme_system
                            ThemeChoice.LIGHT -> R.string.theme_light
                            ThemeChoice.DARK -> R.string.theme_dark
                        },
                    ),
                )
            }
        }
    }
}
