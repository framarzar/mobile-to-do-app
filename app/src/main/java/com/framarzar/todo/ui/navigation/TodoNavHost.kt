package com.framarzar.todo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.framarzar.todo.ui.todos.TodoListScreen

private const val TODO_LIST_ROUTE = "todos"

@Composable
fun TodoNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = TODO_LIST_ROUTE) {
        composable(TODO_LIST_ROUTE) { TodoListScreen() }
    }
}
