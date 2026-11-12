package com.framarzar.todo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.framarzar.todo.ui.edit.EditTodoScreen
import com.framarzar.todo.ui.todos.TodoListScreen

const val TODO_ID_ARG = "todoId"
private const val TODO_LIST_ROUTE = "todos"
private const val EDIT_ROUTE = "edit"

@Composable
fun TodoNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = TODO_LIST_ROUTE) {
        composable(TODO_LIST_ROUTE) {
            TodoListScreen(
                onAddTodo = { navController.navigate(EDIT_ROUTE) },
                onEditTodo = { todoId -> navController.navigate("$EDIT_ROUTE?$TODO_ID_ARG=$todoId") }
            )
        }
        composable(
            route = "$EDIT_ROUTE?$TODO_ID_ARG={$TODO_ID_ARG}",
            arguments = listOf(
                navArgument(TODO_ID_ARG) {
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) {
            EditTodoScreen(onDone = { navController.popBackStack() })
        }
    }
}
