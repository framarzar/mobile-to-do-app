package com.framarzar.todo.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.framarzar.todo.data.TodoRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Alarms do not survive a reboot, so they are set again from the database.
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject lateinit var repository: TodoRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.rescheduleReminders()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
