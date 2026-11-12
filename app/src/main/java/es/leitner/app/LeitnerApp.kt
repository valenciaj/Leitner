package es.leitner.app

import android.app.Application
import es.leitner.app.data.LeitnerDatabase
import es.leitner.app.data.LeitnerRepository
import es.leitner.app.data.MediaStorage

/** Contenedor de dependencias sencillo (sin Hilt de momento). */
class LeitnerApp : Application() {
    val media: MediaStorage by lazy { MediaStorage(this) }
    val repository: LeitnerRepository by lazy {
        LeitnerRepository(LeitnerDatabase.create(this), media)
    }
}
