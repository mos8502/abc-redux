package hu.nemi.abcstore.sample.todo.todos

import dagger.BindsInstance
import dagger.Subcomponent
import hu.nemi.abcstore.sample.todo.lists.ListsComponent

@Subcomponent(modules = [TodosModel::class])
interface TodosComponent {

    fun inject(activity: TodosActivity)

    @Subcomponent.Builder
    interface Builder {
        @BindsInstance
        fun listId(@ListId listId: String): Builder

        fun build(): TodosComponent
    }

    companion object {
        fun inject(activity: TodosActivity) = ListsComponent(activity).todos.listId(activity.intent.getStringExtra(LIST_ID))
                .build()
                .inject(activity)
    }
}