package com.framarzar.todo.ui.todos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.framarzar.todo.R
import com.framarzar.todo.data.INBOX_LIST_ID
import com.framarzar.todo.data.formatDate
import com.framarzar.todo.data.local.TodoEntity
import com.framarzar.todo.data.local.TodoListEntity
import kotlinx.coroutines.launch

private sealed interface ListDialog {
    data object New : ListDialog

    data class Rename(val list: TodoListEntity) : ListDialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoListScreen(
    onAddTodo: () -> Unit,
    onEditTodo: (String) -> Unit,
    viewModel: TodoListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val inboxName = stringResource(R.string.inbox)
    val deletedMessage = stringResource(R.string.todo_deleted)
    val undoLabel = stringResource(R.string.undo)
    var listDialog by remember { mutableStateOf<ListDialog?>(null) }

    LaunchedEffect(Unit) { viewModel.ensureInbox(inboxName) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.todo_list_title)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onAddTodo) {
                Text(stringResource(R.string.add_todo))
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                label = { Text(stringResource(R.string.search)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
            FilterRow(selected = state.filter, onSelected = viewModel::onFilterChange)
            ListRow(
                lists = state.lists,
                selectedListId = state.selectedListId,
                onSelected = viewModel::onListSelected,
                onNewList = { listDialog = ListDialog.New },
                onRenameList = { listDialog = ListDialog.Rename(it) },
                onDeleteList = viewModel::deleteList
            )
            if (state.todos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.todo_list_empty),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            LazyColumn {
                items(state.todos, key = { it.id }) { todo ->
                    TodoRow(
                        todo = todo,
                        onClick = { onEditTodo(todo.id) },
                        onDoneChange = { viewModel.setDone(todo.id, it) },
                        onDelete = {
                            viewModel.deleteTodo(todo.id)
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = deletedMessage,
                                    actionLabel = undoLabel
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.restoreTodo(todo.id)
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    listDialog?.let { dialog ->
        val list = (dialog as? ListDialog.Rename)?.list
        ListNameDialog(
            title = stringResource(if (list == null) R.string.new_list else R.string.rename_list),
            initialName = list?.name.orEmpty(),
            onConfirm = { name ->
                viewModel.saveList(name, list?.id)
                listDialog = null
            },
            onDismiss = { listDialog = null }
        )
    }
}

@Composable
private fun FilterRow(selected: TodoFilter, onSelected: (TodoFilter) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TodoFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onSelected(filter) },
                label = { Text(stringResource(filter.labelRes())) }
            )
        }
    }
}

private fun TodoFilter.labelRes(): Int = when (this) {
    TodoFilter.ALL -> R.string.filter_all
    TodoFilter.ACTIVE -> R.string.filter_active
    TodoFilter.COMPLETED -> R.string.filter_completed
    TodoFilter.DUE_TODAY -> R.string.filter_due_today
    TodoFilter.OVERDUE -> R.string.filter_overdue
}

@Composable
private fun ListRow(
    lists: List<TodoListEntity>,
    selectedListId: String?,
    onSelected: (String?) -> Unit,
    onNewList: () -> Unit,
    onRenameList: (TodoListEntity) -> Unit,
    onDeleteList: (String) -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = selectedListId == null,
            onClick = { onSelected(null) },
            label = { Text(stringResource(R.string.all_lists)) }
        )
        lists.forEach { list ->
            FilterChip(
                selected = list.id == selectedListId,
                onClick = { onSelected(list.id) },
                label = { Text(list.name) }
            )
        }
        AssistChip(onClick = onNewList, label = { Text(stringResource(R.string.new_list)) })
        val selectedList = lists.firstOrNull { it.id == selectedListId }
        if (selectedList != null && selectedList.id != INBOX_LIST_ID) {
            TextButton(onClick = { onRenameList(selectedList) }) {
                Text(stringResource(R.string.rename_list))
            }
            TextButton(onClick = { onDeleteList(selectedList.id) }) {
                Text(stringResource(R.string.delete_list))
            }
        }
    }
}

@Composable
private fun TodoRow(
    todo: TodoEntity,
    onClick: () -> Unit,
    onDoneChange: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = todo.isDone, onCheckedChange = onDoneChange)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = todo.title,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (todo.isDone) TextDecoration.LineThrough else null
            )
            val details = listOfNotNull(
                todo.dueAt?.let { stringResource(R.string.due_on, formatDate(it)) },
                todo.tags.takeIf { it.isNotEmpty() }?.joinToString(" ") { "#$it" }
            )
            if (details.isNotEmpty()) {
                Text(
                    text = details.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        TextButton(onClick = onDelete) { Text(stringResource(R.string.delete)) }
    }
}

@Composable
private fun ListNameDialog(
    title: String,
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.list_name)) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
