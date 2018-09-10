package hu.nemi.abcstore.sample.todo.lists

import android.arch.lifecycle.ViewModel
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import hu.nemi.abcstore.core.Scheduler
import hu.nemi.abcstore.core.StateStore
import hu.nemi.abcstore.sample.todo.todos.TodosComponent
import hu.nemi.abcstore.sample.todo.utils.ViewModelKey
import javax.inject.Singleton

@Module(subcomponents = [TodosComponent::class])
abstract class ListsModule {

    @Binds
    @IntoMap
    @ViewModelKey(ListsViewModel::class)
    abstract  fun bindListsViewModel(viewModel: ListsViewModel): ViewModel

    @Module
    companion object {
        @[Provides Singleton]
        @JvmStatic fun provideRootStateStore(@OnStateChanged onStateChangedScheduler: Scheduler,
                                  @Update updateScheduler: Scheduler): StateStore<List<ListItem>> =
                StateStore(emptyList(), updateScheduler = updateScheduler, stateChangeScheduler = onStateChangedScheduler)
    }

}