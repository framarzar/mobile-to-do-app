package com.framarzar.todo.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "todo_lists")
data class TodoListEntity(
    @PrimaryKey val id: String,
    val name: String,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val dirty: Boolean = true
)

@Entity(tableName = "todos", indices = [Index("listId")])
data class TodoEntity(
    @PrimaryKey val id: String,
    val listId: String,
    val title: String,
    val notes: String = "",
    val isDone: Boolean = false,
    val dueAt: Long? = null,
    val reminderAt: Long? = null,
    val tags: List<String> = emptyList(),
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val dirty: Boolean = true
)
