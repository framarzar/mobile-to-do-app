package com.framarzar.todo.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query(
        "SELECT * FROM todos WHERE deletedAt IS NULL " +
            "ORDER BY isDone, dueAt IS NULL, dueAt, updatedAt DESC"
    )
    fun observeTodos(): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE id = :id")
    suspend fun getById(id: String): TodoEntity?

    @Query("SELECT * FROM todos WHERE listId = :listId AND deletedAt IS NULL")
    suspend fun getByList(listId: String): List<TodoEntity>

    @Query("SELECT * FROM todos WHERE dirty = 1")
    suspend fun getDirty(): List<TodoEntity>

    @Query(
        "SELECT * FROM todos WHERE reminderAt > :now AND isDone = 0 AND deletedAt IS NULL"
    )
    suspend fun getPendingReminders(now: Long): List<TodoEntity>

    @Upsert
    suspend fun upsert(todo: TodoEntity)

    @Query("UPDATE todos SET dirty = 0 WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun markClean(id: String, updatedAt: Long)
}

@Dao
interface TodoListDao {
    @Query("SELECT * FROM todo_lists WHERE deletedAt IS NULL ORDER BY name")
    fun observeLists(): Flow<List<TodoListEntity>>

    @Query("SELECT * FROM todo_lists WHERE id = :id")
    suspend fun getById(id: String): TodoListEntity?

    @Query("SELECT * FROM todo_lists WHERE dirty = 1")
    suspend fun getDirty(): List<TodoListEntity>

    @Upsert
    suspend fun upsert(list: TodoListEntity)

    @Query("UPDATE todo_lists SET dirty = 0 WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun markClean(id: String, updatedAt: Long)
}
