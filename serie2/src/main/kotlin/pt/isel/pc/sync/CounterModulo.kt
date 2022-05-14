package pt.isel.pc.sync

import java.util.concurrent.atomic.AtomicInteger

/**
 * A counter that uses the modulo operator to count.
 * This implementation is is thread-safe.
 */
class CounterModulo(val moduloValue: Int) {

    init {
        require(moduloValue > 0) { "moduloValue must be greater than 0" }
    }

    val value: Int // Counter current value
        get() = atomicValue.get()

    private val atomicValue = AtomicInteger(0)

    fun increment(): Int = setValue { observed ->
        if(observed == moduloValue - 1)
            0
        else
            observed + 1
    }

    fun decrement() = setValue { observed ->
        if(observed == 0)
            moduloValue - 1
        else
            observed - 1
    }

    /**
     * Sets the counter to the given value.
     * Thread-safe.
     * @param computeValue the function that computes the new value, based on the observed one.
     * @return the new value.
     */
    private inline fun setValue(computeValue: (Int) -> Int): Int {
        while (true){
            val observedValue = atomicValue.get()
            val newValue = computeValue(observedValue)
            if(atomicValue.compareAndSet(observedValue, newValue))
                return newValue
        }
    }


}