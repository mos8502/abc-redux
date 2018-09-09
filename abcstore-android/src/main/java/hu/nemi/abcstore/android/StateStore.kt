package hu.nemi.abcstore.android

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.OnLifecycleEvent
import hu.nemi.abcredux.core.Observable
import hu.nemi.abcredux.core.Subscription

fun <S : Any> Observable<S>.subscribe(lifecycleOwner: LifecycleOwner, block: (S) -> Unit): Subscription {
    val observer = StateObserver(this, block)
    lifecycleOwner.lifecycle.addObserver(observer)
    return Subscription {
        lifecycleOwner.lifecycle.removeObserver(observer)
        observer.unsubscribe()
    }
}

fun <S : Any> Observable<S>.asLiveData(): LiveData<S> = ObservableLiveData(this)

private class ObservableLiveData<S : Any>(private val observable: Observable<S>) : LiveData<S>() {
    private var subscription: Subscription? = null

    override fun onActive() {
        super.onActive()
        subscription = observable.subscribe(this::postValue)
    }

    override fun onInactive() {
        super.onInactive()
        subscription?.unsubscribe()
    }

}

private class StateObserver<S : Any>(private val observable: Observable<S>, private val observer: (S) -> Unit) : LifecycleObserver, Subscription {
    private var subscription: Subscription? = null

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {
        subscription = observable.subscribe(observer)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop() {
        subscription?.unsubscribe()
    }

    override fun unsubscribe() {
        subscription?.unsubscribe()
    }
}
