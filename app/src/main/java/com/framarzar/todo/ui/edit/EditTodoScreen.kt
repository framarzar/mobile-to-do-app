package com.framarzar.todo.ui.edit

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.framarzar.todo.R
import com.framarzar.todo.data.datePickerMillisToLocalDate
import com.framarzar.todo.data.formatDate
import com.framarzar.todo.data.formatDateTime
import com.framarzar.todo.data.toEpochMillis
import java.time.LocalDate
import java.time.LocalTime

private enum class Picker { DUE_DATE, REMINDER_DATE, REMINDER_TIME }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTodoScreen(onDone: () -> Unit, viewModel: EditTodoViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var picker by remember { mutableStateOf<Picker?>(null) }
    var reminderDate by remember { mutableStateOf<LocalDate?>(null) }
    val notificationPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (state.isNew) R.string.new_todo else R.string.edit_todo))
                },
                navigationIcon = {
                    TextButton(onClick = onDone) { Text(stringResource(R.string.cancel)) }
                },
                actions = {
                    TextButton(onClick = { viewModel.save(onDone) }) {
                        Text(stringResource(R.string.save))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text(stringResource(R.string.title)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.notes,
                onValueChange = viewModel::onNotesChange,
                label = { Text(stringResource(R.string.notes)) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.lists.forEach { list ->
                    FilterChip(
                        selected = list.id == state.listId,
                        onClick = { viewModel.onListChange(list.id) },
                        label = { Text(list.name) }
                    )
                }
            }
            OutlinedTextField(
                value = state.tagsText,
                onValueChange = viewModel::onTagsChange,
                label = { Text(stringResource(R.string.tags)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            DateField(
                label = stringResource(R.string.due_date),
                value = state.dueAt?.let(::formatDate),
                onPick = { picker = Picker.DUE_DATE },
                onClear = { viewModel.onDueAtChange(null) }
            )
            DateField(
                label = stringResource(R.string.reminder),
                value = state.reminderAt?.let(::formatDateTime),
                onPick = { picker = Picker.REMINDER_DATE },
                onClear = { viewModel.onReminderAtChange(null) }
            )
        }
    }

    when (picker) {
        Picker.DUE_DATE -> DatePickerPopup(
            onPicked = { date ->
                viewModel.onDueAtChange(date.toEpochMillis())
                picker = null
            },
            onDismiss = { picker = null }
        )

        Picker.REMINDER_DATE -> DatePickerPopup(
            onPicked = { date ->
                reminderDate = date
                picker = Picker.REMINDER_TIME
            },
            onDismiss = { picker = null }
        )

        Picker.REMINDER_TIME -> TimePickerPopup(
            onPicked = { time ->
                reminderDate?.let { viewModel.onReminderAtChange(it.toEpochMillis(time)) }
                picker = null
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onDismiss = { picker = null }
        )

        null -> Unit
    }
}

@Composable
private fun DateField(label: String, value: String?, onPick: () -> Unit, onClear: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, modifier = Modifier.weight(1f))
        TextButton(onClick = onPick) { Text(value ?: stringResource(R.string.not_set)) }
        if (value != null) {
            TextButton(onClick = onClear) { Text(stringResource(R.string.clear)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerPopup(onPicked: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val datePickerState = rememberDatePickerState()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis
                        ?.let { onPicked(datePickerMillisToLocalDate(it)) }
                        ?: onDismiss()
                }
            ) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerPopup(onPicked: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val timePickerState = rememberTimePickerState()
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { TimePicker(state = timePickerState) },
        confirmButton = {
            TextButton(
                onClick = { onPicked(LocalTime.of(timePickerState.hour, timePickerState.minute)) }
            ) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
