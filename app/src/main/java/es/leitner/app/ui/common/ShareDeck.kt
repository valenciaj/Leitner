package es.leitner.app.ui.common

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import es.leitner.app.R
import es.leitner.app.data.DeckShare
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Guarda el mazo en un fichero .leitner temporal y abre el menú de compartir de Android
 * (WhatsApp, correo, Drive...). Devuelve false si no se pudo preparar.
 */
suspend fun shareDeck(context: Context, deckShare: DeckShare, deckId: Long, deckName: String): Boolean {
    val file = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.cacheDir, "share").apply { deleteRecursively(); mkdirs() }
            val safeName = deckName.replace(Regex("[^\\p{L}\\p{N} _-]"), "_").trim().ifEmpty { "mazo" }
            File(dir, "$safeName.${DeckShare.EXTENSION}").also { f -> f.outputStream().use { deckShare.export(deckId, it) } }
        }.getOrNull()
    } ?: return false
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "application/octet-stream"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, deckName)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return runCatching {
        context.startActivity(Intent.createChooser(send, context.getString(R.string.share_deck_chooser)))
    }.isSuccess
}
