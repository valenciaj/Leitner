package es.leitner.app.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import es.leitner.app.R

/** Botón de icono con su descripción para TalkBack. */
@Composable
fun ActionIcon(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) { Icon(icon, contentDescription = label) }
}

/** Flecha "Volver" de la barra superior. */
@Composable
fun BackButton(onClick: () -> Unit) =
    ActionIcon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), onClick)

/** Aspa "Salir" de la barra superior. */
@Composable
fun CloseButton(onClick: () -> Unit) =
    ActionIcon(Icons.Filled.Close, stringResource(R.string.exit), onClick)

/** Tres puntos "Más opciones". */
@Composable
fun MoreButton(onClick: () -> Unit) =
    ActionIcon(Icons.Filled.MoreVert, stringResource(R.string.more_options), onClick)
