package hu.nemi.abcstore.sample.todo.utils

import android.os.Handler
import android.os.Looper
import hu.nemi.abcstore.core.Scheduler
import java.util.concurrent.Executors

object UpdateSchuduler : Scheduler {
    private val executor = Executors.newSingleThreadExecutor()
    override fun invoke(task: () -> Unit) {
        executor.submit(task)
    }
}

object StateChangeScheduler : Scheduler {
    private val handler = Handler(Looper.getMainLooper())

    override fun invoke(task: () -> Unit) {
        handler.post(task)
    }
}