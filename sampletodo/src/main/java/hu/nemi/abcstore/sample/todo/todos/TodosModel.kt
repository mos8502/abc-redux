package hu.nemi.abcstore.sample.todo.todos

import android.arch.lifecycle.ViewModel
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import hu.nemi.abcstore.core.Lens
import hu.nemi.abcstore.core.StateStore
import hu.nemi.abcstore.sample.todo.lists.ListItem
import hu.nemi.abcstore.sample.todo.utils.ViewModelKey
import javax.inject.Qualifier

@Qualifier
annotation class ListId

@Module
abstract class TodosModel {

    @Binds
    @IntoMap
    @ViewModelKey(TodosViewModel::class)
    abstract fun bindTodosViewModel(viewModel: TodosViewModel): ViewModel

    @Module
    companion object {
        @Provides
        @JvmStatic
        fun provideTodosStore(@ListId listId: String, store: StateStore<List<ListItem>>): StateStore<Todos> {
            return store.subState("list-$listId") { emptyList<Todo>() }
                    .map(Lens(
                            get = { state -> Todos(listId = listId, listTitle = state.first.first { it.id == listId }.title, todos = state.second) },
                            set = { todos -> { it.copy(first = it.first.map { if(it.id == listId) it.copy(count = todos.todos.size) else it}, second = todos.todos) } }
                    ))
        }
    }
}