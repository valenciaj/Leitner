package es.leitner.app

import android.app.Application
import es.leitner.app.data.AppPrefs
import es.leitner.app.data.BackupManager
import es.leitner.app.data.LeitnerDatabase
import es.leitner.app.data.LeitnerRepository
import es.leitner.app.data.MediaStorage

/** Contenedor de dependencias sencillo (sin Hilt de momento). */
class LeitnerApp : Application() {
    val media: MediaStorage by lazy { MediaStorage(this) }
    private val database: LeitnerDatabase by lazy { LeitnerDatabase.create(this) }
    val repository: LeitnerRepository by lazy { LeitnerRepository(database, media) }
    val prefs: AppPrefs by lazy { AppPrefs(this) }
    val backup: BackupManager by lazy { BackupManager(database, media) }
}
