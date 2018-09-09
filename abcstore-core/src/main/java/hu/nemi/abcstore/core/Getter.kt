package hu.nemi.abcstore.core

/**
 * Optic for getting the focus of the [Getter] from a structure
 */
interface Getter<in S, out A> {
    /**
     * The get operator
     *
     * @param s an object of type [S] from which to get the focus target of type [A]
     * @return the focus target
     */
    operator fun invoke(s: S): A
}