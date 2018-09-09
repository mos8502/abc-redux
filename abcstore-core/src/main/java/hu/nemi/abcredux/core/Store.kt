package hu.nemi.abcredux.core

import kotlin.properties.Delegates

/***
 * Contract for store implementations to dispatch state mutation and state change
 */
interface Scheduler {
    operator fun invoke(task: () -> Unit)
}

/**
 * Contract for action dispatchers.
 */
interface Dispatcher<in S, out R> {

    /**
     * Dispatches an action]
     *
     * @param action of type [S] to dispatch
     * @return arbitrary value returned by the dispatcher
     */
    fun dispatch(action: S): R
}

interface StateScope<T : Any> {
    var value: T
}

/**
 * Basic contract for all stores. A store implements both [Observable<S>] and [Dispatcher<S, Unit>]
 */
interface Store<S : Any, in A : Any> : Dispatcher<A, Unit>, Observable<S> {

    fun withState(f: StateScope<S>.() -> Unit)
}

/**
 * Contract for state stores. State stores are basic stores which maintain a state of type [S] and allow mutation of state only through state mutator functions dispatched to it.
 */
interface StateStore<S : Any> : Store<S, (S) -> S> {

    /**
     * Create a sub state from this store.
     *
     * @param key an arbitrary unique key for the sub state
     * @param init initializer function for the state node
     * @return a [Store<State<S, C>>] that represents both the parent [S] and the child state as a pair]
     */
    fun <C : Any> subState(key: Any, init: () -> C): StateStore<Pair<S, C>>

    /**
     * Map the state represented by this store by a lens. Mapping allows to reshape the state represented by this store
     *
     * @param lens for mapping state
     * @return mapped state store of type [StateStore<T>]
     */
    fun <T : Any> map(lens: Lens<S, T>): StateStore<T>

    /**
     * Create a reducer store which allows to map state through a reducer function based on the messages dispatched to the store
     *
     * @param reducer a function if type <(S, A) -> S> to map the state based on the current state and the action dispatched
     * @return reducer store
     */
    fun <A : Any> withReducer(reducer: (S, A) -> S): Store<S, A>

    companion object {
        /**
         * Factory function to create the root state store
         *
         * @param initialState the initial state of the root state store
         * @return [StateStore<S>]
         */
        operator fun <S : Any> invoke(initialState: S, updateScheduler: Scheduler = ImmediateScheduler, stateChangeScheduler: Scheduler = ImmediateScheduler): StateStore<S> =
                DefaultStateStore(
                        rootStateStore = RootStateStore(initialState = initialState,
                                lock = Lock(),
                                updateScheduler = updateScheduler,
                                stateChangeScheduler = stateChangeScheduler),
                        node = StateNodeRef())
    }
}

private class RootStateStore<R : Any>(initialState: R,
                                      private val lock: Lock,
                                      private val updateScheduler: Scheduler,
                                      private val stateChangeScheduler: Scheduler) {
    private var state by Delegates.observable(StateNode(initialState)) { _, oldState, newState ->
        if (newState != oldState) {
            val subscriptions = this.subscriptions
            stateChangeScheduler {
                subscriptions.forEach { subscriber -> subscriber(newState) }
            }
        }
    }
    @Volatile
    private var subscriptions = emptySet<(StateNode<R>) -> Unit>()
    @Volatile
    private var isDispatching = false

    fun dispatch(action: (StateNode<R>) -> StateNode<R>) = updateScheduler {
        lock {
            if (isDispatching) throw IllegalStateException("an action is already being dispatched")

            isDispatching = true
            state = try {
                action(state)
            } finally {
                isDispatching = false
            }
        }
    }

    fun withState(f: StateScope<StateNode<R>>.() -> Unit) = updateScheduler {
        lock {
            state = object : StateScope<StateNode<R>> {
                override var value: StateNode<R> = state
                    get() = lock {
                        field
                    }
                    set(value) = lock {
                        field = value

                    }
            }.apply(f).value
        }
    }

    fun subscribe(subscriber: (StateNode<R>) -> Unit): Subscription {
        // schedule subscription
        updateScheduler {
            // add subscription and invoke subscriber
            lock {
                subscriptions += subscriber
                subscriber(state)
            }
        }

        // returned subscription with schedule subscribe which will remove the subscriber from the list of subscribers
        return Subscription {
            updateScheduler { subscriptions -= subscriber }
        }
    }
}

private class DefaultStateStore<R : Any, S : Any>(private val rootStateStore: RootStateStore<R>,
                                                  private val node: StateNodeRef<R, S>) : StateStore<S> {
    override fun dispatch(action: (S) -> S) {
        rootStateStore.dispatch { rootState ->
            node.value(rootState, action(node.value(rootState)))
        }
    }

    override fun withState(f: StateScope<S>.() -> Unit) {
        rootStateStore.withState {
            map(node.value).f()
        }
    }

    override fun <C : Any> subState(key: Any, init: () -> C): StateStore<Pair<S, C>> =
            DefaultStateStore(
                    rootStateStore = rootStateStore,
                    node = node.addChild(key, init))

    override fun <T : Any> map(lens: Lens<S, T>): StateStore<T> =
            DefaultStateStore(rootStateStore = rootStateStore,
                    node = node + lens)

    override fun <A : Any> withReducer(reducer: (S, A) -> S): Store<S, A> =
            ReducerStore(this, reducer)

    override fun subscribe(block: (S) -> Unit): Subscription = rootStateStore.subscribe(Subscriber(block, node.value))

    private data class Subscriber<in R : Any, M : Any>(private val block: (M) -> Unit,
                                                       private val state: Lens<StateNode<R>, M>) : (StateNode<R>) -> Unit {
        override fun invoke(rootNode: StateNode<R>) = block(state(rootNode))
    }
}

private class ReducerStore<S : Any, in A : Any>(private val store: Store<S, (S) -> S>,
                                                private val reducer: (S, A) -> S) : Store<S, A> {

    override fun subscribe(block: (S) -> Unit): Subscription = store.subscribe(block)

    override fun dispatch(action: A) {
        store.dispatch { reducer(it, action) }
    }

    override fun withState(f: StateScope<S>.() -> Unit) {
        store.withState(f)
    }
}

private object ImmediateScheduler : Scheduler {
    override fun invoke(task: () -> Unit) {
        task()
    }
}

/**
 * Utility function to map state scope with [Lens]
 */
private fun <P : Any, S : Any> StateScope<P>.map(lens: Lens<P, S>): StateScope<S> = object : StateScope<S> {
    override var value: S
        get() = lens(this@map.value)
        set(value) {
            this@map.value = lens(this@map.value, value)
        }
}
