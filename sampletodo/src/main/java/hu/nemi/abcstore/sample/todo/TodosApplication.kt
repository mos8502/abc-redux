package hu.nemi.abcstore.sample.todo

import android.app.Application
import hu.nemi.abcstore.sample.todo.lists.DaggerListsComponent
import hu.nemi.abcstore.sample.todo.utils.StateChangeScheduler
import hu.nemi.abcstore.sample.todo.utils.UpdateSchuduler

class TodosApplication: Application() {
    val listsComponent = DaggerListsComponent.builder()
            .onStateChangedScheduler(StateChangeScheduler)
            .updateScheduler(UpdateSchuduler)
            .build()
}