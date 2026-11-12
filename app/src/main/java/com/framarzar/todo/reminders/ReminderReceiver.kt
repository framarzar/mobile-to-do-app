package com.framarzar.todo.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.framarzar.todo.MainActivity
import com.framarzar.todo.R
import com.framarzar.todo.data.TodoRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val REMINDER_CHANNEL_ID = "reminders"

fun createReminderChannel(context: Context) {
    val channel = NotificationChannel(
        REMINDER_CHANNEL_ID,
        context.getString(R.string.reminder_channel_name),
        NotificationManager.IMPORTANCE_HIGH
    )
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
}

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var repository: TodoRepository

    override fun onReceive(context: Context, intent: Intent) {
        val todoId = intent.getStringExtra(EXTRA_TODO_ID) ?: return
        when (intent.action) {
            ACTION_SHOW -> showNotification(context, todoId, intent.getStringExtra(EXTRA_TITLE))
            ACTION_DONE -> markDone(context, todoId)
        }
    }

    private fun showNotification(context: Context, todoId: String, title: String?) {
        val canNotify = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!canNotify) return

        val openApp = PendingIntent.getActivity(
            context,
            todoId.hashCode(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val markDone = PendingIntent.getBroadcast(
            context,
            todoId.hashCode(),
            Intent(context, ReminderReceiver::class.java)
                .setAction(ACTION_DONE)
                .putExtra(EXTRA_TODO_ID, todoId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(0, context.getString(R.string.reminder_mark_done), markDone)
            .build()
        NotificationManagerCompat.from(context).notify(todoId.hashCode(), notification)
    }

    private fun markDone(context: Context, todoId: String) {
        NotificationManagerCompat.from(context).cancel(todoId.hashCode())
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.setDone(todoId, true)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_SHOW = "com.framarzar.todo.action.SHOW_REMINDER"
        const val ACTION_DONE = "com.framarzar.todo.action.MARK_DONE"
        const val EXTRA_TODO_ID = "todoId"
        const val EXTRA_TITLE = "title"
    }
}
