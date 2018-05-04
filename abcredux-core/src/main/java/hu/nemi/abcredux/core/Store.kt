package hu.nemi.abcredux.core

import kotlin.properties.Delegates

/**
 * Action factory that can be dispathced to a [StateStore]. When the [ActionCreator] is dispatched the [StateStore] will invoke it with the current state.
 * The [ActionCreator] at this points may choose to return an action or null. If null is returned the state will not be changed
 */
interface ActionCreator<in S : Any, out A> {
    /**
     * Factory function for an action of type [A] give the current state of type [S]
     *
     * @param state the current state
     * @return tha action of type [A] to be dispatched or null
     */
    operator fun invoke(state: S): A?
}

/**
 * Asynchronous action creator. When dispatched to a [StateStore] the store will invoke it with the current state.
 * The [AsyncActionCreator] at this point may dispatch any number of [ActionCreator<S, A>] through the provided dispatch functions
 */
interface AsyncActionCreator<in S : Any, out A : Any?> {
    /**
     * Function to initiate asynchronous [ActionCreator] dispatching
     *
     * @param state the currentstate
     * @param dispatcher dispatcher function to dispatch [ActionCreator<S, A>]s
     */
    operator fun invoke(state: S, dispatcher: (ActionCreator<S, A>) -> Unit)
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

/**
 * Basic contract for all stores. A store implements both [Observable<S>] and [Dispatcher<S, Unit>]
 */
interface Store<out S : Any, in A : Any> : Dispatcher<A, Unit>, Observable<S> {

    /**
     * Dispatches an [ActionCreator<S, A>]
     *
     * @param actionCreator [ActionCreator<S, A>] to dispatch
     */
    fun dispatch(actionCreator: ActionCreator<S, A>)

    /**
     * Dispatches an [AsyncActionCreator<S, A>] to the store
     */
    fun dispatch(asyncActionCreator: AsyncActionCreator<S, A>)
}

/**
 * A middleware is a means of listening to when actions are dispatched to a [Store]. The middleware may choose to dispatch any number of actions back to the store or to alter the action action dispatched to the store itself
 */
interface Middleware<in S, A> {
    /**
     * Invoked when an action is dispatched to the store this [Middleware] is associated with
     *
     * @param store the to which the action has been dispatched to
     * @param state the state at the time the action was dispatched
     * @param next the next dispatcher in the chain
     */
    fun dispatch(store: Dispatcher<A, Unit>, state: S, action: A, next: Dispatcher<A, A?>): A?
}

/**
 * Contract for state stores. State stores are basic stores which maintain a state of type [S] and allow mutation of state only through state mutator functions dispatched to it.
 */
interface StateStore<S : Any> : Store<S, (S) -> S> {

    /**
     * Create a reducer store which allows to mappedBy state through a reducer function based on the messages dispatched to the store
     *
     * @param reducer a function if type <(S, A) -> S> to mappedBy the state based on the current state and the action dispatched
     * @param middleware any number of middlewares to associated with the reducer store
     * @return reducer store
     */
    fun <A : Any> withReducer(reducer: (S, A) -> S, middleware: Iterable<Middleware<S, A>> = emptyList()): Store<S, A>

    /**
     * Zips two stores resulting in an [ImmutableStateStore<Pair<S, T>>]. Zipping doesn't result in a new state node and as such cannot have a sub state
     *
     * @param other the store to zip with
     * @return the zipped store
     */
    fun <T : Any> zip(other: ImmutableStateStore<T>): ImmutableStateStore<Pair<S, T>>

    /**
     * Zips two stores resulting in an [ImmutableStateStore<Pair<S, T>>]. Zipping doesn't result in a new state node and as such cannot have a sub state
     *
     * @param other the store to zip with
     * @return the zipped store
     */
    fun <T : Any> zip(other: MutableStateStore<T>): ImmutableStateStore<Pair<S, T>>

    companion object {
        /**
         * Factory function to create the root state store
         *
         * @param initialState the initial state of the root state store
         * @return [StateStore<S>]
         */
        operator fun <S : Any> invoke(initialState: S): MutableStateStore<S> =
                DefaultMutableStateStore(rootStateStore = RootStateStore(initialState, Lock()), node = MutableStateRef<S, S>(Lens()))
    }
}

interface ImmutableStateStore<S : Any> : StateStore<S> {
    infix fun <T : Any> map(lens: Lens<S, T>): ImmutableStateStore<T>
}

interface MutableStateStore<S : Any> : StateStore<S> {
    infix fun <T : Any> map(lens: Lens<S, T>): MutableStateStore<T>

    fun <C : Any> subState(key: Any, init: () -> C): MutableStateStore<Pair<S, C>>
}

private class MiddlewareDispatcher<in S : Any, A>(private val store: Dispatcher<A, Unit>,
                                                  private val middleware: Iterable<Middleware<S, A>>) : Dispatcher<A, A?> {
    private lateinit var state: S

    fun onStateChanged(state: S) {
        this.state = state
    }

    override fun dispatch(action: A): A? = ActionDispatcher().dispatch(action)

    private inner class ActionDispatcher : Dispatcher<A, A?> {
        private val middlewareIterator = middleware.iterator()
        override fun dispatch(action: A): A? {
            return if (middlewareIterator.hasNext()) middlewareIterator.next().dispatch(store = store, state = state, action = action, next = this)
            else action
        }
    }
}

private class RootStateStore<R : Any>(initialState: R, private val lock: Lock) {
    private var state by Delegates.observable(StateNode(initialState)) { _, oldState, newState ->
        if (newState != oldState) subscriptions.keys.forEach { subscriber -> subscriber(newState) }
    }
    @Volatile
    private var subscriptions = emptyMap<(StateNode<R>) -> Unit, Subscription>()
    @Volatile
    private var isDispatching = false

    fun dispatch(action: (StateNode<R>) -> StateNode<R>) = lock {
        if (isDispatching) throw IllegalStateException("an action is already being dispatched")

        isDispatching = true
        state = try {
            action(state)
        } finally {
            isDispatching = false
        }
    }

    fun dispatch(actionCreator: ActionCreator<StateNode<R>, (StateNode<R>) -> StateNode<R>>) {
        lock {
            actionCreator(state)?.let(::dispatch)
        }
    }

    fun dispatch(asyncActionCreator: AsyncActionCreator<StateNode<R>, (StateNode<R>) -> StateNode<R>>) = lock {
        asyncActionCreator(state) {
            dispatch(it)
        }
    }

    fun subscribe(block: (StateNode<R>) -> Unit): Subscription = lock {
        var subscription = subscriptions[block]
        if (subscription == null) {
            subscription = Subscription {
                subscriptions -= block
            }
            subscriptions += block to subscription
            block(state)
        }
        subscription
    }
}

private class DefaultStateStore<R : Any, S : Any>(private val rootStateStore: RootStateStore<R>,
                                                  private val node: StateRef<R, S>) : StateStore<S> {
    override fun dispatch(action: (S) -> S) {
        rootStateStore.dispatch { rootState ->
            node.value(rootState, action(node.value(rootState)))
        }
    }

    override fun dispatch(actionCreator: ActionCreator<S, (S) -> S>) {
        rootStateStore.dispatch(object : ActionCreator<StateNode<R>, (StateNode<R>) -> StateNode<R>> {
            override fun invoke(state: StateNode<R>) =
                    actionCreator(node.value(state))?.let { action ->
                        { rootNode: StateNode<R> -> node.value(rootNode, action(node.value(rootNode))) }
                    }
        })
    }

    override fun dispatch(asyncActionCreator: AsyncActionCreator<S, (S) -> S>) {
        rootStateStore.dispatch(object : AsyncActionCreator<StateNode<R>, (StateNode<R>) -> StateNode<R>> {
            override fun invoke(state: StateNode<R>, dispatcher: (ActionCreator<StateNode<R>, (StateNode<R>) -> StateNode<R>>) -> Unit) {
                asyncActionCreator(node.value(state)) { dispatch(it) }
            }
        })
    }

    override fun <A : Any> withReducer(reducer: (S, A) -> S, middleware: Iterable<Middleware<S, A>>): Store<S, A> =
            ReducerStore(this, reducer, middleware)

    override fun subscribe(block: (S) -> Unit): Subscription = rootStateStore.subscribe(DefaultSubscriber(block, node.value))

    override fun <T : Any> zip(other: ImmutableStateStore<T>): ImmutableStateStore<Pair<S, T>> {
        val otherStore = other as? DefaultImmutableStateStore<R, T> ?: throw IllegalArgumentException()
        require(otherStore.rootStateStore == rootStateStore)
        return DefaultImmutableStateStore(rootStateStore = rootStateStore, state = node + otherStore.state)
    }

    override fun <T : Any> zip(other: MutableStateStore<T>): ImmutableStateStore<Pair<S, T>> {
        val otherStore = other as? DefaultMutableStateStore<R, T> ?: throw IllegalArgumentException()
        require(otherStore.rootStateStore == rootStateStore)
        return DefaultImmutableStateStore(rootStateStore = rootStateStore, state = node + otherStore.node)
    }
}

private class DefaultMutableStateStore<R : Any, S : Any>(val rootStateStore: RootStateStore<R>,
                                                         val node: MutableStateRef<R, S>) : StateStore<S> by DefaultStateStore(rootStateStore, node), MutableStateStore<S> {

    override fun <C : Any> subState(key: Any, init: () -> C): MutableStateStore<Pair<S, C>> =
            DefaultMutableStateStore(
                    rootStateStore = rootStateStore,
                    node = node.addChild(key, init))

    override fun <T : Any> map(lens: Lens<S, T>): MutableStateStore<T> = DefaultMutableStateStore(rootStateStore = rootStateStore, node = node + lens)
}

private class DefaultImmutableStateStore<R : Any, S : Any>(val rootStateStore: RootStateStore<R>,
                                                           val state: StateRef<R, S>) : StateStore<S> by DefaultStateStore(rootStateStore, state), ImmutableStateStore<S> {

    override fun <T : Any> map(lens: Lens<S, T>): ImmutableStateStore<T> = DefaultImmutableStateStore(rootStateStore = rootStateStore, state = state + lens)
}

private class ReducerStore<S : Any, in A : Any>(private val store: Store<S, (S) -> S>,
                                                internal val reducer: (S, A) -> S,
                                                middleware: Iterable<Middleware<S, A>>) : Store<S, A> {
    private val middlewareDispatcher = MiddlewareDispatcher(this, middleware).apply {
        subscribe(::onStateChanged)
    }

    override fun subscribe(block: (S) -> Unit): Subscription = store.subscribe(block)

    override fun dispatch(action: A) {
        middlewareDispatcher.dispatch(action)?.let { dispatchedAction ->
            store.dispatch { reducer(it, dispatchedAction) }
        }
    }

    override fun dispatch(actionCreator: ActionCreator<S, A>) {
        store.dispatch(object : ActionCreator<S, (S) -> S> {
            override fun invoke(state: S): ((S) -> S)? = actionCreator(state)?.let { action ->
                { reducer(state, action) }
            }
        })
    }

    override fun dispatch(asyncActionCreator: AsyncActionCreator<S, A>) {
        store.dispatch(object : AsyncActionCreator<S, (S) -> S> {
            override fun invoke(state: S, dispatcher: (ActionCreator<S, (S) -> S>) -> Unit) {
                asyncActionCreator(state) { actionCreator ->
                    dispatch(actionCreator)
                }
            }
        })
    }
}

private data class DefaultSubscriber<in R : Any, S : Any>(private val block: (S) -> Unit,
                                                          private val get: Getter<StateNode<R>, S>) : (StateNode<R>) -> Unit {

    override fun invoke(stateRoot: StateNode<R>) = block(get(stateRoot))
}
