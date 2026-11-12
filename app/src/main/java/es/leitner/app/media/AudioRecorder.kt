package es.leitner.app.media

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/** Graba audio de micrófono en un fichero .m4a. */
class AudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null

    val isRecording: Boolean get() = recorder != null

    fun start(output: File) {
        stop()
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setAudioSamplingRate(44_100)
        r.setAudioEncodingBitRate(96_000)
        r.setOutputFile(output.absolutePath)
        r.prepare()
        r.start()
        recorder = r
    }

    /** Para la grabación. Devuelve false si la grabación no es válida (p. ej. demasiado corta). */
    fun stop(): Boolean {
        val r = recorder ?: return false
        recorder = null
        return try {
            r.stop()
            true
        } catch (e: RuntimeException) {
            false
        } finally {
            r.release()
        }
    }
}
