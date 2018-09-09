package hu.nemi.abcstore.core

/**
 * Optic for setting or modifying a focus target
 */
interface Setter<S, A> {
    /**
     * Operator for setting focus target of type [A] in object of type [S]
     *
     * @param s target object [S]
     * @param a focus target [A]
     * @return updated object of type [S]
     */
    operator fun invoke(s: S, a: A): S

    /**
     * Operator for modifying target of type [A] in object of type [S]
     *
     * @param s object to modify of type [S]
     * @param f transformation function to modify target of type [A]
     * @return updated object of type [S]
     */
    operator fun invoke(s: S, f: (A) -> A): S
}
