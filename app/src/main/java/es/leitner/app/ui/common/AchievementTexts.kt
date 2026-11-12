package es.leitner.app.ui.common

import androidx.annotation.StringRes
import es.leitner.app.R
import es.leitner.core.Achievement

/** Nombre de la pegatina. */
@get:StringRes
val Achievement.titleRes: Int
    get() = when (this) {
        Achievement.FIRST_CORRECT -> R.string.ach_first_correct
        Achievement.CORRECT_10 -> R.string.ach_correct_10
        Achievement.CORRECT_50 -> R.string.ach_correct_50
        Achievement.CORRECT_100 -> R.string.ach_correct_100
        Achievement.CORRECT_500 -> R.string.ach_correct_500
        Achievement.CORRECT_1000 -> R.string.ach_correct_1000
        Achievement.STREAK_3 -> R.string.ach_streak_3
        Achievement.STREAK_7 -> R.string.ach_streak_7
        Achievement.STREAK_30 -> R.string.ach_streak_30
        Achievement.DAYS_10 -> R.string.ach_days_10
        Achievement.DAYS_50 -> R.string.ach_days_50
    }

/** Pista para conseguir la pegatina. */
@get:StringRes
val Achievement.hintRes: Int
    get() = when (this) {
        Achievement.FIRST_CORRECT -> R.string.ach_first_correct_hint
        Achievement.CORRECT_10 -> R.string.ach_correct_10_hint
        Achievement.CORRECT_50 -> R.string.ach_correct_50_hint
        Achievement.CORRECT_100 -> R.string.ach_correct_100_hint
        Achievement.CORRECT_500 -> R.string.ach_correct_500_hint
        Achievement.CORRECT_1000 -> R.string.ach_correct_1000_hint
        Achievement.STREAK_3 -> R.string.ach_streak_3_hint
        Achievement.STREAK_7 -> R.string.ach_streak_7_hint
        Achievement.STREAK_30 -> R.string.ach_streak_30_hint
        Achievement.DAYS_10 -> R.string.ach_days_10_hint
        Achievement.DAYS_50 -> R.string.ach_days_50_hint
    }
