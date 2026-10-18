package com.example.taksy.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.example.taksy.MainActivity
import com.example.taksy.R
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH = "com.example.taksy.widget.ACTION_REFRESH"

        fun refreshAll(context: Context) {
            val intent = Intent(context, TaskWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_IDS,
                    AppWidgetManager.getInstance(context).getAppWidgetIds(
                        ComponentName(context, TaskWidgetProvider::class.java)
                    )
                )
            }
            context.sendBroadcast(intent)
        }
    }

    /**
     * Room no deja leer en el hilo principal (lanza IllegalStateException y cierra la app), y
     * onUpdate se ejecuta en él: el contador se lee en segundo plano con goAsync. Se llama tanto
     * al añadir el widget como tras cada cambio de tarea (refreshAll).
     */
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        if (appWidgetIds.isEmpty()) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val count = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    WidgetEntryPoint::class.java
                ).taskDao().getPendingTasksSync().size
                for (appWidgetId in appWidgetIds) {
                    updateWidget(context, appWidgetManager, appWidgetId, count)
                }
                // Recarga las filas de la lista además de la cabecera
                @Suppress("DEPRECATION")
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_task_list)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val mgr = AppWidgetManager.getInstance(context)
            onUpdate(context, mgr, mgr.getAppWidgetIds(ComponentName(context, TaskWidgetProvider::class.java)))
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        count: Int
    ) {
        val serviceIntent = Intent(context, TaskWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }

        val views = RemoteViews(context.packageName, R.layout.widget_task_list)

        @Suppress("DEPRECATION")
        views.setRemoteAdapter(R.id.widget_task_list, serviceIntent)
        views.setEmptyView(R.id.widget_task_list, R.id.widget_empty)

        val countText = context.resources.getQuantityString(R.plurals.widget_task_count, count, count)
        views.setTextViewText(R.id.widget_count, countText)

        val openAppIntent = Intent(context, MainActivity::class.java)
        val openAppPending = PendingIntent.getActivity(
            context, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_header, openAppPending)

        val itemClickIntent = Intent(context, MainActivity::class.java)
        val itemClickPending = PendingIntent.getActivity(
            context, 1, itemClickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setPendingIntentTemplate(R.id.widget_task_list, itemClickPending)

        val refreshIntent = Intent(context, TaskWidgetProvider::class.java).apply {
            action = ACTION_REFRESH
        }
        val refreshPending = PendingIntent.getBroadcast(
            context, 2, refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_refresh, refreshPending)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
