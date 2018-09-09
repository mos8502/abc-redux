package hu.nemi.abcstore.core

/**
 * Optic for modifying immutable data structures. It implements [Getter] and [Setter] to be able to get and set a focus target if type [A] from object of type [S]
 */
interface Lens<S, A> : Getter<S, A>, Setter<S, A> {

    /**
     * Plus operator allowing composition of [Lens]'. Lens<A, B> + Lens<B, C> = Lens<A, C>
     * @param other the lens to compose with this lens
     * @return the composed lens
     */
    operator fun <V> plus(other: Lens<A, V>): Lens<S, V>

    /**
     * Factory for creating default instances of the default implementation of a [Lens]
     */
    companion object {
        /**
         * Factory function to create a lens of type [Lens<S, A>]
         *
         * @param get function to retrieve the focus target of type [A] from object of type [S]
         * @param set function to update focus target of type [A] in object of type [S]
         *
         * @return [Lens<S, A>]
         */
        operator fun <S, A> invoke(get: (S) -> A, set: (A) -> (S) -> S): Lens<S, A> = DefaultLens(get = get, set = set)

        /**
         * Factory function for creating an identity [Lens<S, S>]
         *
         * @return identity [Lens<S, S>]
         */
        operator fun <S> invoke(): Lens<S, S> = DefaultLens(
                get = { it },
                set = { it -> { _ -> it } }
        )
    }
}

private class DefaultLens<S, A>(val get: (S) -> A, val set: (A) -> (S) -> S) : Lens<S, A> {
    override fun invoke(s: S): A = get(s)

    override fun invoke(s: S, a: A): S = set(a)(s)

    override fun <V> plus(other: Lens<A, V>): Lens<S, V> = Lens(get = { other(this(it)) }, set = { v: V -> { s: S -> this(s, other(this(s), v)) } })

    override fun invoke(s: S, f: (A) -> A): S = invoke(s, f(invoke(s)))
}
