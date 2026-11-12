package com.framarzar.todo.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.framarzar.todo.MainActivity
import com.framarzar.todo.R
import com.framarzar.todo.data.TodoRepository
import com.framarzar.todo.data.local.TodoEntity
import com.framarzar.todo.data.startOfTomorrow
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

private val todoIdKey = ActionParameters.Key<String>("todoId")

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun todoRepository(): TodoRepository
}

private fun repository(context: Context): TodoRepository =
    EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java).todoRepository()

class TodoWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = repository(context)
        provideContent {
            val todos by repository.todos.collectAsState(initial = emptyList())
            val dueOrOverdue = todos.filter { todo ->
                !todo.isDone && todo.dueAt != null && todo.dueAt < startOfTomorrow()
            }
            GlanceTheme { WidgetContent(context, dueOrOverdue) }
        }
    }
}

@Composable
private fun WidgetContent(context: Context, todos: List<TodoEntity>) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .padding(12.dp)
    ) {
        Text(
            text = context.getString(R.string.widget_title),
            style = TextStyle(fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onSurface),
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .clickable(actionStartActivity<MainActivity>())
        )
        if (todos.isEmpty()) {
            Text(
                text = context.getString(R.string.widget_empty),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant)
            )
        }
        LazyColumn {
            items(todos, itemId = { it.id.hashCode().toLong() }) { todo ->
                CheckBox(
                    checked = false,
                    onCheckedChange = actionRunCallback<CompleteTodoAction>(
                        actionParametersOf(todoIdKey to todo.id)
                    ),
                    text = todo.title,
                    style = TextStyle(color = GlanceTheme.colors.onSurface)
                )
            }
        }
    }
}

class CompleteTodoAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val todoId = parameters[todoIdKey] ?: return
        repository(context).setDone(todoId, true)
    }
}

class TodoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodoWidget()
}

@Singleton
class WidgetUpdater @Inject constructor(@ApplicationContext private val context: Context) {
    suspend fun update() = TodoWidget().updateAll(context)
}
