package hu.nemi.abcstore.core

import com.nhaarman.mockito_kotlin.inOrder
import com.nhaarman.mockito_kotlin.mock
import org.junit.Test

class ReducerStoreTest {

    @Test
    fun `action dispatched to reducer store updates state`() {
        val store = StateStore(23)
        val reducerStore = store.withReducer(reducer = reducer)
        val storeSubscriber: (Int) -> Unit = mock()
        val reducerStoreSubscriber: (Int) -> Unit = mock()

        store.subscribe(storeSubscriber)
        reducerStore.subscribe(reducerStoreSubscriber)
        reducerStore.dispatch(Op.Inc)

        inOrder(storeSubscriber, reducerStoreSubscriber) {
            verify(storeSubscriber).invoke(23)
            verify(reducerStoreSubscriber).invoke(23)
            verify(storeSubscriber).invoke(24)
            verify(reducerStoreSubscriber).invoke(24)
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun `action dispatched to state store updates state`() {
        val store = StateStore(23)
        val reducerStore = store.withReducer(reducer = reducer)
        val storeSubscriber: (Int) -> Unit = mock()
        val reducerStoreSubscriber: (Int) -> Unit = mock()

        store.subscribe(storeSubscriber)
        reducerStore.subscribe(reducerStoreSubscriber)
        store.dispatch { it * 2 }

        inOrder(storeSubscriber, reducerStoreSubscriber) {
            verify(storeSubscriber).invoke(23)
            verify(reducerStoreSubscriber).invoke(23)
            verify(storeSubscriber).invoke(46)
            verify(reducerStoreSubscriber).invoke(46)
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun `state changes not dispatched to reducer store after unsubscribed`() {
        val store = StateStore(23)
        val reducerStore = store.withReducer(reducer = reducer)
        val storeSubscriber: (Int) -> Unit = mock()
        val reducerStoreSubscriber: (Int) -> Unit = mock()

        store.subscribe(storeSubscriber)
        reducerStore.subscribe(reducerStoreSubscriber).unsubscribe()
        reducerStore.dispatch(Op.Inc)

        inOrder(storeSubscriber, reducerStoreSubscriber) {
            verify(storeSubscriber).invoke(23)
            verify(reducerStoreSubscriber).invoke(23)
            verify(storeSubscriber).invoke(24)
            verifyNoMoreInteractions()
        }
    }
}

private val reducer: (Int, Op) -> Int = { state, action ->
    when (action) {
        Op.Inc -> state + 1
        Op.Dec -> state - 1
        is Op.Set -> action.value
    }
}

private sealed class Op {
    object Inc : Op()
    object Dec : Op()
    data class Set(val value: Int) : Op()
}