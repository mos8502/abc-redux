package hu.nemi.abcstore.core

import com.nhaarman.mockito_kotlin.inOrder
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import org.junit.Test

class StateStoreTest {

    @Test
    fun `state immediately emitted when subscribed`() {
        val subscriber: (Int) -> Unit = mock()
        val store = StateStore(23)

        store.subscribe(subscriber)

        verify(subscriber)(23)
        verifyNoMoreInteractions(subscriber)
    }

    @Test
    fun `subscriber not notified after unsubscribing`() {
        val subscriber: (Int) -> Unit = mock()
        val store = StateStore(23)
        val subscription = store.subscribe(subscriber)

        subscription.unsubscribe()
        store.dispatch { it * 2 }

        verify(subscriber)(23)
        verifyNoMoreInteractions(subscriber)
    }

    @Test
    fun `subscribers gets notified if state changes`() {
        val subscriber: (Int) -> Unit = mock()
        val store = StateStore(23)

        store.subscribe(subscriber)
        store.dispatch { it * 2 }

        inOrder(subscriber) {
            verify(subscriber)(23)
            verify(subscriber)(46)
            verifyNoMoreInteractions(subscriber)
        }
    }

    @Test
    fun `state not emitted when not changed`() {
        val subscriber: (Int) -> Unit = mock()
        val store = StateStore(23)

        store.subscribe(subscriber)
        store.dispatch { 23 }

        inOrder(subscriber) {
            verify(subscriber)(23)
            verifyNoMoreInteractions(subscriber)
        }
    }

    @Test
    fun `can have child state`() {
        val subscriber: (Int) -> Unit = mock()
        val store = StateStore(23)
        val childSubscriber: (String) -> Unit = mock()
        val childStore = store.subState("string") { "Hello, World!" }
                .map(Lens(
                        get = { it.second },
                        set = { value: String -> { state: Pair<Int, String> -> state.copy(second = value) } }
                ))
        val grandchildSubscriber: (Long) -> Unit = mock()
        val grandChildStore = childStore.subState("long") { -1L }
                .map(Lens(
                        get = { it.second },
                        set = { value: Long -> { state: Pair<String, Long> -> state.copy(second = value) } }
                ))

        store.subscribe(subscriber)
        childStore.subscribe(childSubscriber)
        grandChildStore.subscribe(grandchildSubscriber)

        store.dispatch { it * 2 }
        childStore.dispatch { "Goodbye, World!" }
        grandChildStore.dispatch { it + 23L }

        inOrder(subscriber, childSubscriber, grandchildSubscriber) {
            verify(subscriber)(23)
            verify(childSubscriber)("Hello, World!")
            verify(grandchildSubscriber)(-1L)
            verify(subscriber)(46)
            verify(childSubscriber)("Goodbye, World!")
            verify(grandchildSubscriber)(22L)
        }
    }

    @Test
    fun `deep states`() {
        data class First(val root: Pair<Pair<Pair<Pair<Int, String>, Int>, Long>, Unit>, val fifth: Pair<Int, Int>)
        data class Second(val root: Pair<Pair<Pair<Int, String>, Int>, Long>, val fourth: Unit, val fifth: Pair<Int, Int>)
        data class Third(val root: Pair<Pair<Int, String>, Int>, val third: Long, val fourth: Unit, val fifth: Pair<Int, Int>)
        data class Fourth(val root: Pair<Int, String>, val second: Int, val third: Long, val fourth: Unit, val fifth: Pair<Int, Int>)
        data class FlattenedState(val root: Int, val first: String, val second: Int, val third: Long, val fourth: Unit, val fifth: Pair<Int, Int>)

        val flatten =
                Lens<Pair<Pair<Pair<Pair<Pair<Int, String>, Int>, Long>, Unit>, Pair<Int, Int>>, First>(
                        get = { First(root = it.first, fifth = it.second) },
                        set = { first -> { state -> state.copy(first = first.root) } }
                ) +
                        Lens<First, Second>(
                                get = { Second(root = it.root.first, fourth = it.root.second, fifth = it.fifth) },
                                set = { second -> { first -> first.copy(root = Pair(second.root, second.fourth), fifth = second.fifth) } }
                        ) +
                        Lens<Second, Third>(
                                get = { Third(root = it.root.first, third = it.root.second, fourth = it.fourth, fifth = it.fifth) },
                                set = { third -> { second -> second.copy(root = Pair(third.root, third.third), fourth = third.fourth, fifth = third.fifth) } }
                        ) +
                        Lens<Third, Fourth>(
                                get = { Fourth(root = it.root.first, second = it.root.second, third = it.third, fourth = it.fourth, fifth = it.fifth) },
                                set = { fourth -> { third -> third.copy(root = Pair(fourth.root, fourth.second), third = fourth.third, fourth = fourth.fourth, fifth = fourth.fifth) } }
                        ) +
                        Lens<Fourth, FlattenedState>(
                                get = { FlattenedState(root = it.root.first, first = it.root.second, second = it.second, third = it.third, fourth = it.fourth, fifth = it.fifth) },
                                set = { state -> { fourth -> fourth.copy(root = Pair(state.root, state.first), second = state.second, third = state.third, fourth = state.fourth, fifth = state.fifth) } }
                        )

        val store = StateStore(23)
                .subState("first") { "1" }
                .subState("second") { 2 }
                .subState("third") { 3L }
                .subState(4) { Unit }
                .subState(5L) { Pair(1, 2) }
        val subscriber: (Pair<Pair<Pair<Pair<Pair<Int, String>, Int>, Long>, Unit>, Pair<Int, Int>>) -> Unit = mock()
        val expected = Pair(Pair(Pair(Pair(Pair(23, "1"), 2), 3L), Unit), 1 to 2)

        val mappedStore = store.map(flatten)
        val mappedSubscriber: (FlattenedState) -> Unit = mock()

        store.subscribe(subscriber)
        mappedStore.subscribe(mappedSubscriber)
        verify(subscriber)(expected)
        verify(mappedSubscriber)(flatten(expected))

    }
}