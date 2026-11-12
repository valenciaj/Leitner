package es.leitner.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Guarda imágenes y audios de las tarjetas dentro de la app (filesDir/media),
 * para que sigan existiendo aunque el usuario borre el original.
 */
class MediaStorage(context: Context) {

    private val appContext = context.applicationContext
    private val dir = File(appContext.filesDir, "media").apply { mkdirs() }

    /** Fichero nuevo y vacío para grabar audio. */
    fun newAudioFile(): File = File(dir, "${UUID.randomUUID()}.m4a")

    /** Copia la imagen elegida, reducida a un máximo de [maxSide] px. Devuelve la ruta o null si falla. */
    suspend fun importImage(uri: Uri, maxSide: Int = 1280): String? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = appContext.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= maxSide || bounds.outHeight / (sample * 2) >= maxSide) sample *= 2
            val bitmap = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: return@runCatching null
            val file = File(dir, "${UUID.randomUUID()}.jpg")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            bitmap.recycle()
            file.absolutePath
        }.getOrNull()
    }

    /** Borra un fichero, solo si está dentro de la carpeta de la app. */
    fun delete(path: String) {
        val file = File(path)
        if (file.canonicalPath.startsWith(dir.canonicalPath)) file.delete()
    }
}
