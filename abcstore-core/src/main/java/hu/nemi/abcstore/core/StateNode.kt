package hu.nemi.abcstore.core

internal data class StateNode<out V : Any>(val value: V, val children: Map<Any, StateNode<*>> = emptyMap())

internal interface StateNodeRef<R : Any, V : Any> {
    val value: Lens<StateNode<R>, V>

    operator fun <T : Any> plus(lens: Lens<V, T>): StateNodeRef<R, T>

    fun <C : Any> addChild(key: Any, init: () -> C): StateNodeRef<R, Pair<V, C>>

    companion object {
        operator fun <R : Any> invoke(): StateNodeRef<R, R> = DefaultStateNodeRef(Lens())
    }
}

private data class DefaultStateNodeRef<R : Any, V : Any>(val nodeLens: Lens<StateNode<R>, StateNode<V>>) : StateNodeRef<R, V> {

    override val value: Lens<StateNode<R>, V> = nodeLens + Lens(
            get = { it.value },
            set = { value -> { node -> node.copy(value = value) } }
    )

    override fun <C : Any> addChild(key: Any, init: () -> C): StateNodeRef<R, Pair<V, C>> = DefaultStateNodeRef(
            nodeLens + Lens(
                    get = {
                        val childNode = (it.children[key] as? StateNode<C>)
                                ?: StateNode(value = init())
                        StateNode(value = Pair(first = it.value, second = childNode.value), children = childNode.children)
                    },
                    set = { childNode ->
                        { parentNode ->
                            parentNode.copy(value = childNode.value.first, children = parentNode.children + (key to StateNode(value = childNode.value.second, children = childNode.children)))
                        }
                    }
            )
    )

    override fun <T : Any> plus(lens: Lens<V, T>): StateNodeRef<R, T> = DefaultStateNodeRef(
            nodeLens + Lens(
                    get = { node: StateNode<V> -> StateNode(value = lens(node.value), children = node.children) },
                    set = { mappedNode: StateNode<T> -> { node: StateNode<V> -> node.copy(value = lens(node.value, mappedNode.value), children = mappedNode.children) } }
            )
    )
}
