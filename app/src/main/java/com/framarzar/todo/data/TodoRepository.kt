package com.framarzar.todo.data

import com.framarzar.todo.data.local.TodoDao
import com.framarzar.todo.data.local.TodoEntity
import com.framarzar.todo.data.local.TodoListDao
import com.framarzar.todo.data.local.TodoListEntity
import com.framarzar.todo.data.sync.SyncScheduler
import com.framarzar.todo.reminders.ReminderScheduler
import com.framarzar.todo.widget.WidgetUpdater
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

// Fixed id so the default list merges across devices instead of duplicating.
const val INBOX_LIST_ID = "inbox"

@Singleton
class TodoRepository @Inject constructor(
    private val todoDao: TodoDao,
    private val listDao: TodoListDao,
    private val reminderScheduler: ReminderScheduler,
    private val syncScheduler: SyncScheduler,
    private val widgetUpdater: WidgetUpdater
) {
    val todos: Flow<List<TodoEntity>> = todoDao.observeTodos()
    val lists: Flow<List<TodoListEntity>> = listDao.observeLists()

    suspend fun ensureInbox(defaultName: String) {
        if (listDao.getById(INBOX_LIST_ID) == null) {
            listDao.upsert(TodoListEntity(id = INBOX_LIST_ID, name = defaultName, updatedAt = 0))
        }
    }

    suspend fun getTodo(id: String): TodoEntity? = todoDao.getById(id)

    suspend fun saveTodo(todo: TodoEntity) {
        val saved = todo.copy(updatedAt = System.currentTimeMillis(), dirty = true)
        todoDao.upsert(saved)
        reminderScheduler.schedule(saved)
        onLocalChange()
    }

    suspend fun setDone(id: String, isDone: Boolean) {
        val todo = todoDao.getById(id) ?: return
        saveTodo(todo.copy(isDone = isDone))
    }

    suspend fun deleteTodo(id: String) {
        val todo = todoDao.getById(id) ?: return
        saveTodo(todo.copy(deletedAt = System.currentTimeMillis()))
    }

    suspend fun restoreTodo(id: String) {
        val todo = todoDao.getById(id) ?: return
        saveTodo(todo.copy(deletedAt = null))
    }

    suspend fun saveList(name: String, id: String? = null) {
        listDao.upsert(
            TodoListEntity(
                id = id ?: UUID.randomUUID().toString(),
                name = name,
                updatedAt = System.currentTimeMillis()
            )
        )
        onLocalChange()
    }

    suspend fun deleteList(id: String) {
        if (id == INBOX_LIST_ID) return
        val list = listDao.getById(id) ?: return
        val now = System.currentTimeMillis()
        todoDao.getByList(id).forEach { deleteTodo(it.id) }
        listDao.upsert(list.copy(deletedAt = now, updatedAt = now, dirty = true))
        onLocalChange()
    }

    suspend fun rescheduleReminders() {
        todoDao.getPendingReminders(System.currentTimeMillis()).forEach(reminderScheduler::schedule)
    }

    private suspend fun onLocalChange() {
        syncScheduler.requestSync()
        widgetUpdater.update()
    }
}
