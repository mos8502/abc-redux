package hu.nemi.abcstore.core

internal data class StateNode<V : Any, R : Any>(val value: V, val subscribers: Set<(oldState: StateNode<R, R>) -> Unit> = emptySet(), val children: Map<Any, StateNode<*, R>> = emptyMap())

internal interface StateNodeRef<R : Any, V : Any> {
    val value: Lens<StateNode<R, R>, V>
    val subscribers: Lens<StateNode<R, R>, Set<(StateNode<R, R>) -> Unit>>

    operator fun <T : Any> plus(lens: Lens<V, T>): StateNodeRef<R, T>

    fun <C : Any> addChild(key: Any, init: () -> C): StateNodeRef<R, Pair<V, C>>

    companion object {
        operator fun <R : Any> invoke(): StateNodeRef<R, R> = DefaultStateNodeRef(Lens())
    }
}

private data class DefaultStateNodeRef<R : Any, V : Any>(val nodeLens: Lens<StateNode<R, R>, StateNode<V, R>>) : StateNodeRef<R, V> {

    override val value: Lens<StateNode<R, R>, V> = nodeLens + Lens(
            get = { it.value },
            set = { value -> { node -> node.copy(value = value) } }
    )

    override val subscribers: Lens<StateNode<R, R>, Set<(StateNode<R, R>) -> Unit>> = nodeLens + Lens(
            get = { it.subscribers },
            set = { subscribers ->
                { node ->
                    node.copy(subscribers = subscribers)
                }
            }
    )

    override fun <C : Any> addChild(key: Any, init: () -> C): StateNodeRef<R, Pair<V, C>> = DefaultStateNodeRef(
            nodeLens + Lens(
                    get = {
                        val childNode = (it.children[key] as? StateNode<C, R>)
                                ?: StateNode(value = init())
                        StateNode(value = Pair(first = it.value, second = childNode.value), children = childNode.children, subscribers = childNode.subscribers)
                    },
                    set = { childNode ->
                        { parentNode ->
                            parentNode.copy(value = childNode.value.first, children = parentNode.children + (key to StateNode(value = childNode.value.second, children = childNode.children, subscribers = childNode.subscribers)))
                        }
                    }
            )
    )

    override fun <T : Any> plus(lens: Lens<V, T>): StateNodeRef<R, T> = DefaultStateNodeRef(
            nodeLens + Lens(
                    get = { node: StateNode<V, R> -> StateNode(value = lens(node.value), children = node.children, subscribers = node.subscribers) },
                    set = { mappedNode: StateNode<T, R> -> { node: StateNode<V, R> -> node.copy(value = lens(node.value, mappedNode.value), children = mappedNode.children, subscribers = mappedNode.subscribers) } }
            )
    )
}
