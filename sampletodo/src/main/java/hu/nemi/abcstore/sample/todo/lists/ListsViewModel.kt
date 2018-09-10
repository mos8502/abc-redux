package hu.nemi.abcstore.sample.todo.lists

import android.arch.lifecycle.ViewModel
import hu.nemi.abcstore.android.asLiveData
import hu.nemi.abcstore.core.StateStore
import java.util.*
import javax.inject.Inject

class ListsViewModel @Inject constructor(private val store: StateStore<List<ListItem>>) : ViewModel() {
    val lists = store.asLiveData()

    fun addItem(title: String) {
        store.dispatch {
            it + ListItem(id = UUID.randomUUID().toString(), title = title, count = 0)
        }
    }

    fun removeItem(id: String) {
        store.dispatch {
            it.filter { item -> item.id != id }
        }
    }

}