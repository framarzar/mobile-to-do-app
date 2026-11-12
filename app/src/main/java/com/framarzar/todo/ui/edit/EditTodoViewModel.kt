package com.framarzar.todo.ui.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.framarzar.todo.data.INBOX_LIST_ID
import com.framarzar.todo.data.TodoRepository
import com.framarzar.todo.data.local.TodoEntity
import com.framarzar.todo.data.local.TodoListEntity
import com.framarzar.todo.ui.navigation.TODO_ID_ARG
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditTodoUiState(
    val isNew: Boolean = true,
    val title: String = "",
    val notes: String = "",
    val tagsText: String = "",
    val listId: String = INBOX_LIST_ID,
    val dueAt: Long? = null,
    val reminderAt: Long? = null,
    val lists: List<TodoListEntity> = emptyList()
)

@HiltViewModel
class EditTodoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: TodoRepository
) : ViewModel() {
    private val todoId: String? = savedStateHandle[TODO_ID_ARG]
    private var existing: TodoEntity? = null

    private val _uiState = MutableStateFlow(EditTodoUiState(isNew = todoId == null))
    val uiState: StateFlow<EditTodoUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val lists = repository.lists.first()
            existing = todoId?.let { repository.getTodo(it) }
            _uiState.update { state ->
                val todo = existing ?: return@update state.copy(lists = lists)
                state.copy(
                    title = todo.title,
                    notes = todo.notes,
                    tagsText = todo.tags.joinToString(", "),
                    listId = todo.listId,
                    dueAt = todo.dueAt,
                    reminderAt = todo.reminderAt,
                    lists = lists
                )
            }
        }
    }

    fun onTitleChange(title: String) = _uiState.update { it.copy(title = title) }

    fun onNotesChange(notes: String) = _uiState.update { it.copy(notes = notes) }

    fun onTagsChange(tagsText: String) = _uiState.update { it.copy(tagsText = tagsText) }

    fun onListChange(listId: String) = _uiState.update { it.copy(listId = listId) }

    fun onDueAtChange(dueAt: Long?) = _uiState.update { it.copy(dueAt = dueAt) }

    fun onReminderAtChange(reminderAt: Long?) = _uiState.update { it.copy(reminderAt = reminderAt) }

    fun save(onSaved: () -> Unit) {
        val state = _uiState.value
        if (state.title.isBlank()) return
        val base = existing ?: TodoEntity(
            id = UUID.randomUUID().toString(),
            listId = state.listId,
            title = "",
            updatedAt = 0
        )
        viewModelScope.launch {
            repository.saveTodo(
                base.copy(
                    listId = state.listId,
                    title = state.title.trim(),
                    notes = state.notes.trim(),
                    tags = state.tagsText.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                    dueAt = state.dueAt,
                    reminderAt = state.reminderAt
                )
            )
            onSaved()
        }
    }
}
