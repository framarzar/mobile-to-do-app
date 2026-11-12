package com.framarzar.todo.data.sync

import android.content.Context
import androidx.core.content.edit
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.framarzar.todo.data.local.TodoDao
import com.framarzar.todo.data.local.TodoEntity
import com.framarzar.todo.data.local.TodoListDao
import com.framarzar.todo.data.local.TodoListEntity
import com.framarzar.todo.reminders.ReminderScheduler
import com.framarzar.todo.widget.WidgetUpdater
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

private const val PREFS = "sync"
private const val KEY_LISTS_SYNCED_AT = "listsSyncedAt"
private const val KEY_TODOS_SYNCED_AT = "todosSyncedAt"
private const val UPDATED_AT = "updatedAt"

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val todoDao: TodoDao,
    private val listDao: TodoListDao,
    private val reminderScheduler: ReminderScheduler,
    private val widgetUpdater: WidgetUpdater
) : CoroutineWorker(context, params) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override suspend fun doWork(): Result {
        val firebaseIsConfigured = FirebaseApp.getApps(applicationContext).isNotEmpty()
        if (!firebaseIsConfigured) return Result.success()
        return try {
            sync()
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun sync() {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser ?: auth.signInAnonymously().await().user ?: return
        val userDoc = FirebaseFirestore.getInstance().collection("users").document(user.uid)
        val remoteLists = userDoc.collection("lists")
        val remoteTodos = userDoc.collection("todos")

        // Pull before push so a newer remote edit is never overwritten by an older local one.
        pullLists(remoteLists)
        pullTodos(remoteTodos)
        pushLists(remoteLists)
        pushTodos(remoteTodos)
        widgetUpdater.update()
    }

    private suspend fun pullLists(remote: CollectionReference) {
        val changed = changedSince(remote, KEY_LISTS_SYNCED_AT)
        changed.forEach { doc ->
            val incoming = doc.toListEntity()
            val local = listDao.getById(incoming.id)
            if (local == null || incoming.updatedAt > local.updatedAt) listDao.upsert(incoming)
        }
        rememberSyncedAt(KEY_LISTS_SYNCED_AT, changed)
    }

    private suspend fun pullTodos(remote: CollectionReference) {
        val changed = changedSince(remote, KEY_TODOS_SYNCED_AT)
        changed.forEach { doc ->
            val incoming = doc.toTodoEntity()
            val local = todoDao.getById(incoming.id)
            if (local == null || incoming.updatedAt > local.updatedAt) {
                todoDao.upsert(incoming)
                reminderScheduler.schedule(incoming)
            }
        }
        rememberSyncedAt(KEY_TODOS_SYNCED_AT, changed)
    }

    private suspend fun changedSince(remote: CollectionReference, key: String) =
        remote.whereGreaterThan(UPDATED_AT, prefs.getLong(key, -1)).get().await().documents

    private fun rememberSyncedAt(key: String, changed: List<DocumentSnapshot>) {
        val newest = changed.maxOfOrNull { it.getLong(UPDATED_AT) ?: 0 } ?: return
        prefs.edit { putLong(key, newest) }
    }

    private suspend fun pushLists(remote: CollectionReference) {
        listDao.getDirty().forEach { list ->
            remote.document(list.id).set(list.toRemote()).await()
            listDao.markClean(list.id, list.updatedAt)
        }
    }

    private suspend fun pushTodos(remote: CollectionReference) {
        todoDao.getDirty().forEach { todo ->
            remote.document(todo.id).set(todo.toRemote()).await()
            todoDao.markClean(todo.id, todo.updatedAt)
        }
    }
}

private fun TodoListEntity.toRemote() = mapOf(
    "name" to name,
    UPDATED_AT to updatedAt,
    "deletedAt" to deletedAt
)

private fun DocumentSnapshot.toListEntity() = TodoListEntity(
    id = id,
    name = getString("name").orEmpty(),
    updatedAt = getLong(UPDATED_AT) ?: 0,
    deletedAt = getLong("deletedAt"),
    dirty = false
)

private fun TodoEntity.toRemote() = mapOf(
    "listId" to listId,
    "title" to title,
    "notes" to notes,
    "isDone" to isDone,
    "dueAt" to dueAt,
    "reminderAt" to reminderAt,
    "tags" to tags,
    UPDATED_AT to updatedAt,
    "deletedAt" to deletedAt
)

private fun DocumentSnapshot.toTodoEntity() = TodoEntity(
    id = id,
    listId = getString("listId").orEmpty(),
    title = getString("title").orEmpty(),
    notes = getString("notes").orEmpty(),
    isDone = getBoolean("isDone") ?: false,
    dueAt = getLong("dueAt"),
    reminderAt = getLong("reminderAt"),
    tags = (get("tags") as? List<*>).orEmpty().filterIsInstance<String>(),
    updatedAt = getLong(UPDATED_AT) ?: 0,
    deletedAt = getLong("deletedAt"),
    dirty = false
)
