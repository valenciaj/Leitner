package es.leitner.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import es.leitner.app.LeitnerApp
import es.leitner.app.MainActivity
import es.leitner.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Widget de la pantalla de inicio: cuántas tarjetas quedan hoy (de los dos modos). Al tocarlo abre la app. */
class DueWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        scope.launch {
            try {
                render(context, manager, ids)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /** Repinta todos los widgets. Se llama al salir de la app y desde el recordatorio diario. */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, DueWidget::class.java))
            if (ids.isEmpty()) return
            scope.launch { render(context.applicationContext, manager, ids) }
        }

        private suspend fun render(context: Context, manager: AppWidgetManager, ids: IntArray) {
            val due = (context.applicationContext as LeitnerApp).repository.totalDue()
            val open = PendingIntent.getActivity(
                context,
                1,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val views = RemoteViews(context.packageName, R.layout.widget_due).apply {
                setTextViewText(R.id.widget_count, if (due > 0) due.toString() else "✓")
                setTextViewText(
                    R.id.widget_label,
                    context.getString(
                        when (due) {
                            0 -> R.string.widget_label_none
                            1 -> R.string.widget_label_one
                            else -> R.string.widget_label_many
                        },
                    ),
                )
                setOnClickPendingIntent(R.id.widget_root, open)
            }
            ids.forEach { manager.updateAppWidget(it, views) }
        }
    }
}
