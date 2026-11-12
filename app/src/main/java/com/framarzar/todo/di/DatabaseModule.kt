package com.framarzar.todo.di

import android.content.Context
import androidx.room.Room
import com.framarzar.todo.data.local.TodoDao
import com.framarzar.todo.data.local.TodoDatabase
import com.framarzar.todo.data.local.TodoListDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TodoDatabase =
        Room.databaseBuilder(context, TodoDatabase::class.java, "todos.db").build()

    @Provides
    fun provideTodoDao(database: TodoDatabase): TodoDao = database.todoDao()

    @Provides
    fun provideTodoListDao(database: TodoDatabase): TodoListDao = database.todoListDao()
}
