package es.leitner.app.reminder

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import es.leitner.core.ReminderTime
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/** Programa (o cancela) el aviso diario con WorkManager. Sobrevive a reinicios del móvil. */
object ReminderScheduler {
    private const val WORK_NAME = "daily_reminder"

    fun schedule(context: Context, hour: Int, minute: Int) {
        val delay = ReminderTime.delayUntil(LocalDateTime.now(), hour, minute)
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delay.toMinutes(), TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
