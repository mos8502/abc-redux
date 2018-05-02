package hu.nemi.abcredux.core

/**
 * Contract for subscriptions to an arbitrary source, allowing the the expression of the intent that the subscription is no longer required
 */
interface Subscription {
    /**
     * Unsubscribe from the source
     */
    fun unsubscribe()

    companion object {
        /**
         * Factor function to return a default implementation of [Subscription]. The default implementation guarantees the the unsubscribe event is only sent once and only once guarded by the provided lock
         *
         * @param lock an instance of [Lock] to guarantee that we only unsubscribe once and only once from the source
         * @param onUnsubscribe block of code to invoke to perform the actual unsubscribe logic
         */
        operator fun invoke(lock: Lock = Lock(), onUnsubscribe: () -> Unit): Subscription =
                DefaultSubscription(lock, onUnsubscribe)
    }
}

/**
 * Contract for observable data sources
 * */
interface Observable<out T> {
    /**
     * Subscribe to this [Observable] providing a code block to be invoked when the observed data changes
     *
     * @param block code block to be invoked when the observed data changes
     * @return [Subscription] to allowing the consumer to unsubscribe from this [Observable]
     */
    fun subscribe(block: (T) -> Unit): Subscription
}

private class DefaultSubscription(private val lock: Lock,
                                  private val onUnsubscribe: () -> Unit) : Subscription {
    @Volatile
    private var isUnsubscribed = false

    override fun unsubscribe() = lock {
        if (!isUnsubscribed) {
            try {
                onUnsubscribe()
            } finally {
                isUnsubscribed = true
            }
        }
    }
}