package hu.nemi.abcstore.core

/**
 * Helper function to fold reducer functions of [(S, A) -> S] to a single reducer function of the same type
 *
 * @param reducers reducers to fold
 * @return the folded reducer function
 * @throws [IllegalArgumentException] if no functions are being passed
 */
fun <S: Any, A: Any> fold(vararg reducers: (S, A) -> S): (S, A) -> S {
    require(reducers.isNotEmpty()) { "no reducers passed" }
    return { state, action ->
        reducers.fold(state) { state, reducer ->
            reducer.invoke(state, action)
        }
    }
}
