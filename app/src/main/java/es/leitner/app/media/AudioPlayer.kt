package es.leitner.app.media

import android.media.MediaPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/** Reproductor sencillo ligado al ciclo de vida del composable. */
class AudioPlayer {
    private var player: MediaPlayer? = null
    var isPlaying by mutableStateOf(false)
        private set

    fun play(path: String) {
        stop()
        player = runCatching {
            MediaPlayer().apply {
                setDataSource(path)
                setOnCompletionListener { this@AudioPlayer.stop() }
                prepare()
                start()
            }
        }.getOrNull()
        isPlaying = player != null
    }

    fun stop() {
        player?.runCatching { stop() }
        player?.release()
        player = null
        isPlaying = false
    }
}

@Composable
fun rememberAudioPlayer(): AudioPlayer {
    val player = remember { AudioPlayer() }
    DisposableEffect(player) { onDispose { player.stop() } }
    return player
}
