package com.example.taksy.widget

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.taksy.R
import com.example.taksy.data.AppDatabase
import com.example.taksy.data.Task
import com.example.taksy.data.TaskPrioridad
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return TaskWidgetRemoteViewsFactory(applicationContext)
    }
}

class TaskWidgetRemoteViewsFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private var tasks: List<Task> = emptyList()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        val db = AppDatabase.getDatabase(context)
        tasks = db.taskDao().getPendingTasksSync()
    }

    override fun onDestroy() {
        tasks = emptyList()
    }

    override fun getCount(): Int = tasks.size

    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_task_item)
        if (position >= tasks.size) return views

        val task = tasks[position]

        views.setTextViewText(R.id.widget_task_title, task.titulo)

        val dotColor = when (task.prioridad) {
            TaskPrioridad.ALTA -> context.getColor(R.color.widget_priority_high)
            TaskPrioridad.MEDIA -> context.getColor(R.color.widget_priority_medium)
            TaskPrioridad.BAJA -> context.getColor(R.color.widget_priority_low)
            TaskPrioridad.NINGUNA -> context.getColor(R.color.widget_priority_default)
        }
        views.setInt(R.id.widget_priority_dot, "setColorFilter", dotColor)

        if (task.fechaVencimiento != null) {
            val fmt = SimpleDateFormat("dd MMM", Locale.getDefault())
            views.setTextViewText(R.id.widget_task_date, fmt.format(task.fechaVencimiento))
            views.setViewVisibility(R.id.widget_task_date, View.VISIBLE)

            if (task.fechaVencimiento.before(Date())) {
                views.setTextColor(R.id.widget_task_date, context.getColor(R.color.widget_priority_high))
            } else {
                views.setTextColor(R.id.widget_task_date, context.getColor(R.color.widget_text_secondary))
            }
        } else {
            views.setViewVisibility(R.id.widget_task_date, View.GONE)
        }

        views.setOnClickFillInIntent(R.id.widget_item_root, Intent())

        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = if (position < tasks.size) tasks[position].id else position.toLong()
    override fun hasStableIds(): Boolean = true
}
