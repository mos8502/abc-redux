package hu.nemi.abcredux.core

/**
 * Data class to represent a path from a node in the state tree to the root node
 */
data class State<out P : Any, out S : Any>(val parentState: P, val state: S)

internal data class StateNode<out V : Any>(val value: V, val children: Map<Any, StateNode<*>> = emptyMap())

internal interface StateRef<R : Any, V : Any> {
    val value: Lens<StateNode<R>, V>

    operator fun <T : Any> plus(lens: Lens<V, T>): StateRef<R, T>

    operator fun <T : Any> plus(other: StateRef<R, T>): StateRef<R, Pair<V, T>> = StateRef(Lens(
            get = { value(it) to other.value(it) },
            set = { pair -> { node -> other.value(value(node, pair.first), pair.second) } }
    ))

    companion object {
        operator fun <R : Any, V : Any> invoke(value: Lens<StateNode<R>, V>): StateRef<R, V> = object : StateRef<R, V> {
            override val value: Lens<StateNode<R>, V> = value

            override fun <T : Any> plus(lens: Lens<V, T>): StateRef<R, T> = StateRef(value + lens)

        }
    }
}

internal interface MutableStateRef<R : Any, V : Any> : StateRef<R, V> {
    fun <C : Any> addChild(key: Any, init: () -> C): MutableStateRef<R, State<V, C>>

    override operator fun <T : Any> plus(lens: Lens<V, T>): MutableStateRef<R, T>

    companion object {
        operator fun <R : Any, V : Any> invoke(nodeLens: Lens<StateNode<R>, StateNode<V>>): MutableStateRef<R, V> = object : MutableStateRef<R, V> {
            override val value: Lens<StateNode<R>, V> = nodeLens + Lens(
                    get = { it.value },
                    set = { value -> { node -> node.copy(value = value) } }
            )

            override fun <C : Any> addChild(key: Any, init: () -> C): MutableStateRef<R, State<V, C>> = MutableStateRef(
                    nodeLens + Lens(
                            get = {
                                val childNode = (it.children[key] as? StateNode<C>) ?: StateNode(value = init())
                                StateNode(value = State(parentState = it.value, state = childNode.value), children = childNode.children)
                            },
                            set = { childNode ->
                                { parentNode ->
                                    parentNode.copy(value = childNode.value.parentState, children = parentNode.children + (key to StateNode(value = childNode.value.state, children = childNode.children)))
                                }

                            }
                    )
            )

            override fun <T : Any> plus(lens: Lens<V, T>): MutableStateRef<R, T> = MutableStateRef(
                    nodeLens + Lens(
                            get = { node: StateNode<V> -> StateNode(value = lens(node.value), children = node.children) },
                            set = { mappedNode: StateNode<T> -> { node: StateNode<V> -> node.copy(value = lens(node.value, mappedNode.value), children = mappedNode.children) } }
                    )
            )
        }
    }
}
