package hu.nemi.abcredux.core

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import org.junit.Test

class WithStateTest {

    @Test
    fun `value set in withState updates state and respect distinct state changes`() {
        val store = StateStore(23)
        val subStore = store.subState("sub") { 2 }
        val storeSubscriber: (Int) -> Unit = mock()
        val subStoreSubscriber: (Pair<Int, Int>) -> Unit = mock()

        store.subscribe(storeSubscriber)
        subStore.subscribe(subStoreSubscriber)

        subStore.withState { value = 13 to 1 }
        store.withState { value = 42 }
        store.withState {
            (value..value + 100).forEach { value = it }
            value = 53
        }

        store.withState { value = 53 }
        subStore.withState { value = 53 to 1 }

        verify(storeSubscriber)(23)
        verify(storeSubscriber)(13)
        verify(storeSubscriber)(42)
        verify(storeSubscriber)(53)

        verify(subStoreSubscriber)(23 to 2)
        verify(subStoreSubscriber)(13 to 1)
        verify(subStoreSubscriber)(42 to 1)
        verify(subStoreSubscriber)(53 to 1)

        verifyNoMoreInteractions(storeSubscriber, subStoreSubscriber)
    }
}