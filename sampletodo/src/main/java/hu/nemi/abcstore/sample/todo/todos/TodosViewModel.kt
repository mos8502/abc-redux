package hu.nemi.abcstore.sample.todo.todos

import android.arch.lifecycle.ViewModel
import hu.nemi.abcstore.android.asLiveData
import hu.nemi.abcstore.core.StateStore
import java.util.*
import javax.inject.Inject

class TodosViewModel @Inject constructor(private val store: StateStore<Todos>) : ViewModel() {
    val todos = store.asLiveData()

    fun addItem(text: String) {
        store.dispatch { it.copy(todos = it.todos + Todo(id = UUID.randomUUID().toString(), text = text)) }
    }

    fun removeItem(id: String) {
        store.dispatch { it.copy(todos = it.todos.filter { todo -> todo.id != id }) }
    }
}