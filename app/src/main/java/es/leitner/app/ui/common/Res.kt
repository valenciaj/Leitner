package es.leitner.app.ui.common

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource

/** Texto en plural según [count] (p. ej. "1 tarjeta" / "3 tarjetas"). */
@Composable
fun pluralResource(@PluralsRes id: Int, count: Int, vararg args: Any): String {
    LocalConfiguration.current // vuelve a leerse si cambia el idioma
    val resources = LocalContext.current.resources
    return resources.getQuantityString(id, count, *(if (args.isEmpty()) arrayOf<Any>(count) else args))
}

/** Elige el texto del modo niños o del modo normal. */
@Composable
fun modeString(kids: Boolean, @StringRes kidsId: Int, @StringRes normalId: Int, vararg args: Any): String =
    stringResource(if (kids) kidsId else normalId, *args)
