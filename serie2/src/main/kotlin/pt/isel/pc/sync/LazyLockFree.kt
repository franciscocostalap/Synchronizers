package pt.isel.pc.sync

import pt.isel.pc.sync.LazyLockFree.State.*
import java.util.concurrent.atomic.AtomicReference

/**
 * A thread safe and lock free.
 */
class LazyLockFree<T>(initializer: ()->T) {

    private val lazyState = AtomicReference<State>(UNINITIALIZED)

    /**
     * State of the object.
     *
     * @property UNINITIALIZED The object has not been initialized.
     * @property INITIALIZED The object has been initialized, or is initializing.
     */
    private enum class State {
        UNINITIALIZED,
        INITIALIZED
    }

    // Initializer function as a property, maintains the interface given.
    private val supplier = initializer

    // Needs to be volatile because there is no happens before relationship with lazyState
    @Volatile private var _value: T? = null
    val value: T
        get() {
            val obsState = lazyState.get()
            return if (obsState == UNINITIALIZED && lazyState.compareAndSet(UNINITIALIZED, INITIALIZED)) {
                supplier().also { _value = it }
            }else {
                // If CAS fails, we are in INITIALIZED state for sure.
                // Need to wait for the value computation to finish
                while (_value == null) Thread.yield()
                _value!!
            }
        }
}



