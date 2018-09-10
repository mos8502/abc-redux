package hu.nemi.abcstore.core

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
    @Volatile private var state = StateNode<R, R>(initialState)
    @Volatile private var isDispatching = false

    fun dispatch(action: (StateNode<R, R>) -> StateNode<R, R>) = updateScheduler {
        lock {
            if (isDispatching) throw IllegalStateException("an action is already being dispatched")

            isDispatching = true
            try {
                setState(action(state))
            } finally {
                isDispatching = false
            }
        }
    }

    fun withState(f: StateScope<StateNode<R, R>>.() -> Unit) = updateScheduler {
        lock {
            setState(StateScopeImpl(lock, state).apply(f).value)
        }
    }

    fun <S : Any> subscribe(block: (S) -> Unit, node: StateNodeRef<R, S>): Subscription {
        val subscriber = Subscriber(block, node)

        updateScheduler {
            val state = node.subscribers(state) { subscribers ->
                subscribers + subscriber
            }
            this.state = state

            stateChangeScheduler {
                subscriber(state)
            }
        }

        return Subscription {
            this.state = node.subscribers(this.state) { subscribers ->
                subscribers - subscriber
            }
        }
    }

    private fun dispatchStateChanged(state: StateNode<R, R>, node: StateNode<*, R>) {
        node.subscribers.forEach { onStateChanged -> onStateChanged(state) }
        node.children.values.forEach { childNode -> dispatchStateChanged(state, childNode) }
    }

    private fun setState(newState: StateNode<R, R>) {
        if (state != newState) {
            state = newState
            stateChangeScheduler {
                dispatchStateChanged(newState, newState)
            }
        }
    }

    private class StateScopeImpl<R : Any>(private val lock: Lock, initialValue: R) : StateScope<R> {
        override var value: R = initialValue
            get() = lock {
                field
            }
            set(value) = lock {
                field = value
            }
    }

    private class Subscriber<R : Any, S : Any>(private val block: (S) -> Unit, private val nodeRef: StateNodeRef<R, S>) : (StateNode<R, R>) -> Unit {
        override fun invoke(state: StateNode<R, R>) {
            block(nodeRef.value(state))
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Subscriber<*, *>

            if (block != other.block) return false

            return true
        }

        override fun hashCode(): Int {
            return block.hashCode()
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

    override fun subscribe(block: (S) -> Unit): Subscription = rootStateStore.subscribe(block, node)
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
