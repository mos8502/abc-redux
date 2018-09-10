package hu.nemi.abcstore.sample.todo.lists

import dagger.BindsInstance
import dagger.Component
import hu.nemi.abcstore.core.Scheduler
import hu.nemi.abcstore.sample.todo.utils.ViewModelModule
import javax.inject.Qualifier
import javax.inject.Singleton
import android.content.Context
import hu.nemi.abcstore.sample.todo.TodosApplication
import hu.nemi.abcstore.sample.todo.todos.TodosComponent

@Qualifier
annotation class Update

@Qualifier
annotation class OnStateChanged

@Singleton
@Component(modules = [ViewModelModule::class, ListsModule::class])
interface ListsComponent {

    fun inject(activity: ListsActivity)

    val todos: TodosComponent.Builder

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun onStateChangedScheduler(@OnStateChanged scheduler: Scheduler):  Builder

        @BindsInstance
        fun updateScheduler(@Update scheduler: Scheduler): Builder


        fun build(): ListsComponent
    }

    companion object {
        operator fun invoke(context: Context): ListsComponent = (context.applicationContext as TodosApplication).listsComponent
    }
}