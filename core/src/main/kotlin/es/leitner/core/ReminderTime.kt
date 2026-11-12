package es.leitner.core

import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/** Cálculos para el recordatorio diario. */
object ReminderTime {

    /** Tiempo que falta desde [now] hasta la próxima vez que sea [hour]:[minute]. */
    fun delayUntil(now: LocalDateTime, hour: Int, minute: Int): Duration {
        require(hour in 0..23 && minute in 0..59)
        var next = now.with(LocalTime.of(hour, minute))
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next)
    }

    fun format(hour: Int, minute: Int): String = "%02d:%02d".format(hour, minute)
}
