package hu.nemi.abcredux.core

import java.util.ConcurrentModificationException
import java.util.concurrent.atomic.AtomicLong

/**
 * Lock contract. A lock will guarantee that while a block of code is executed under the lock no one else may acquire it
 */
interface Lock {
    /**
     * Lock operator to execute code block holding this lock
     *
     * @param block the block of code to be executed under the lock with return type [R]
     * @return value returned by the block
     */
    operator fun <R> invoke(block: () -> R): R

    companion object {
        /**
         * Factory function to return a [Lock] instance. The default implementation will not block access but will raise [ConcurrentModificationException] if the lock is attempted to be acquire when it is currently being held
         */
        operator fun invoke(): Lock = DefaultLock()
    }
}

private class DefaultLock : Lock {
    private val accessingThread = AtomicLong(-1L)
    @Volatile private var accessCount = 0

    override fun <R> invoke(block: () -> R): R {
        val threadId = Thread.currentThread().id
        return if (accessingThread.get() == threadId || accessingThread.compareAndSet(-1L, threadId)) {
            ++accessCount
            try {
                block()
            } finally {
                if (--accessCount == 0) accessingThread.set(-1L)
            }
        } else {
            throw ConcurrentModificationException()
        }
    }
}