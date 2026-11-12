package es.leitner.app.ui.mode

import es.leitner.app.ui.ModeNames
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.leitner.core.Mode

@Composable
fun ModeSelectScreen(onModeSelected: (Mode) -> Unit, onOpenSettings: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("¿Quién va a estudiar?", style = MaterialTheme.typography.headlineMedium)

            Button(
                onClick = { onModeSelected(Mode.KIDS) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7043)),
                modifier = Modifier.fillMaxWidth().height(120.dp),
            ) {
                Text("🧒  ${ModeNames.KIDS}", fontSize = 28.sp)
            }

            OutlinedButton(
                onClick = { onModeSelected(Mode.NORMAL) },
                modifier = Modifier.fillMaxWidth().height(72.dp),
            ) {
                Text(ModeNames.NORMAL, fontSize = 20.sp)
            }

            TextButton(onClick = onOpenSettings) { Text("⚙ Ajustes de cajas") }
        }
    }
}
