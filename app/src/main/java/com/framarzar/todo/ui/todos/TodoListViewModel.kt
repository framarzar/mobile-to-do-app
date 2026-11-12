package com.framarzar.todo.ui.todos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.framarzar.todo.data.TodoRepository
import com.framarzar.todo.data.local.TodoEntity
import com.framarzar.todo.data.local.TodoListEntity
import com.framarzar.todo.data.startOfToday
import com.framarzar.todo.data.startOfTomorrow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TodoFilter { ALL, ACTIVE, COMPLETED, DUE_TODAY, OVERDUE }

data class TodoListUiState(
    val todos: List<TodoEntity> = emptyList(),
    val lists: List<TodoListEntity> = emptyList(),
    val query: String = "",
    val filter: TodoFilter = TodoFilter.ACTIVE,
    val selectedListId: String? = null
)

private data class Criteria(
    val query: String = "",
    val filter: TodoFilter = TodoFilter.ACTIVE,
    val selectedListId: String? = null
)

private const val STOP_TIMEOUT_MILLIS = 5_000L

@HiltViewModel
class TodoListViewModel @Inject constructor(private val repository: TodoRepository) : ViewModel() {
    private val criteria = MutableStateFlow(Criteria())

    val uiState: StateFlow<TodoListUiState> =
        combine(repository.todos, repository.lists, criteria) { todos, lists, criteria ->
            TodoListUiState(
                todos = todos.filter { it.matches(criteria) },
                lists = lists,
                query = criteria.query,
                filter = criteria.filter,
                selectedListId = criteria.selectedListId
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            TodoListUiState()
        )

    fun ensureInbox(defaultName: String) {
        viewModelScope.launch { repository.ensureInbox(defaultName) }
    }

    fun onQueryChange(query: String) = criteria.update { it.copy(query = query) }

    fun onFilterChange(filter: TodoFilter) = criteria.update { it.copy(filter = filter) }

    fun onListSelected(listId: String?) = criteria.update { it.copy(selectedListId = listId) }

    fun setDone(todoId: String, isDone: Boolean) {
        viewModelScope.launch { repository.setDone(todoId, isDone) }
    }

    fun deleteTodo(todoId: String) {
        viewModelScope.launch { repository.deleteTodo(todoId) }
    }

    fun restoreTodo(todoId: String) {
        viewModelScope.launch { repository.restoreTodo(todoId) }
    }

    fun saveList(name: String, listId: String?) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.saveList(name.trim(), listId) }
    }

    fun deleteList(listId: String) {
        onListSelected(null)
        viewModelScope.launch { repository.deleteList(listId) }
    }
}

private fun TodoEntity.matches(criteria: Criteria): Boolean {
    val inList = criteria.selectedListId == null || listId == criteria.selectedListId
    val matchesQuery = criteria.query.isBlank() ||
        title.contains(criteria.query, ignoreCase = true) ||
        notes.contains(criteria.query, ignoreCase = true) ||
        tags.any { it.contains(criteria.query, ignoreCase = true) }
    val matchesFilter = when (criteria.filter) {
        TodoFilter.ALL -> true
        TodoFilter.ACTIVE -> !isDone
        TodoFilter.COMPLETED -> isDone
        TodoFilter.DUE_TODAY -> !isDone && dueAt != null && dueAt in startOfToday()..<startOfTomorrow()
        TodoFilter.OVERDUE -> !isDone && dueAt != null && dueAt < startOfToday()
    }
    return inList && matchesQuery && matchesFilter
}
