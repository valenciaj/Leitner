package es.leitner.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import es.leitner.app.R
import es.leitner.app.data.AppPrefs
import es.leitner.core.Mode

/** Objetivo diario del modo elegido: cuántas tarjetas repasar al día (0 = sin objetivo). */
@Composable
fun DailyGoalSection(prefs: AppPrefs, mode: Mode) {
    val goals by prefs.dailyGoals.collectAsState()
    val goal = goals[mode] ?: 0
    val step = AppPrefs.DAILY_GOAL_STEP
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.goal_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.goal_help), style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val less = stringResource(R.string.goal_less)
            val more = stringResource(R.string.goal_more)
            OutlinedButton(
                onClick = { prefs.setDailyGoal(mode, goal - step) },
                enabled = goal > 0,
                modifier = Modifier.semantics { contentDescription = less },
            ) { Text("−") }
            Text(
                if (goal == 0) stringResource(R.string.goal_off) else stringResource(R.string.goal_value, goal),
                style = MaterialTheme.typography.titleMedium,
            )
            OutlinedButton(
                onClick = { prefs.setDailyGoal(mode, goal + step) },
                enabled = goal < AppPrefs.MAX_DAILY_GOAL,
                modifier = Modifier.semantics { contentDescription = more },
            ) { Text("+") }
        }
    }
}
