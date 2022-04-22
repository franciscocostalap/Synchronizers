package pt.isel.pc.sync


import pt.isel.pc.utils.awaitUntil
import pt.isel.pc.utils.dueTime
import pt.isel.pc.utils.isPast
import pt.isel.pc.utils.isZero
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration

/**

    This synchronizer supports the exchange of information between pairs of threads.
    Threads that use this synchronizer manifest their willingness to initiate an exchange by invoking the exchange method,
    specifying the object they want to deliver to the partner thread (value)
    and the limit duration of the wait for the exchange (timeout)

    Implemented as a lock-based synchronizer using the kernel style approach.
    Makes one exchange at a time.
 */
class Exchanger<T> {

    private val monitor = ReentrantLock()

    /**
     * Represents a consumer of the exchange. (The thread that comes after the waiting thread)
     * @property value the value delivered by the partner thread
     * @property condition the condition used for specific signaling
     */
    private inner class Waiter(var value: T?=null, val condition: Condition)

    /**
     * Holder that holds the values making part of the exchange
     */
    private inner class ExchangerHolder(var v1: T?, var v2:T?)
    private val emptyHolder = ExchangerHolder(null, null)

    private var exchangeHolder: ExchangerHolder = emptyHolder

    /**
     * Thread that waits for its partner to complete the exchange
     */
    private var waiterThread: Waiter? = null

    @Throws(InterruptedException::class)
    fun exchange(value: T, timeout: Duration): T?{
        monitor.withLock {

            /**
             * Second thread always takes the fast path
             */
            if(waiterThread != null){
                val v = exchangeHolder.v1
                exchangeHolder.v2 = value
                notifyTraderWith(value)
                return v
            }
            if(timeout.isZero) return null

            // Wait path
            val scopeWaiter = Waiter(null, condition=monitor.newCondition())
            this.waiterThread = scopeWaiter
            exchangeHolder.v1 = value
            val dueTime = timeout.dueTime()

            while(true){
                try {
                    scopeWaiter.condition awaitUntil dueTime
                    // If the partner thread has already delivered the value, return it
                    if (scopeWaiter.value != null) {
                        return scopeWaiter.value
                    }
                    if (dueTime.isPast) {
                        // remove waiter if timeout was reached so other exchange can take place
                        this.waiterThread = null
                        return null
                    }
                }catch (e: InterruptedException){
                    if(scopeWaiter.value != null) {
                        Thread.currentThread().interrupt()
                        return scopeWaiter.value
                    }
                    // remove waiter if the thread was interrupted so other exchange can take place
                    waiterThread = null
                    throw e
                }
            }
        }
    }

    /**
     * Delivers and notifies the [waiterThread] the value was delivered
     * @param value the value to be delivered
     */
    private fun notifyTraderWith(value: T) {
        checkNotNull(waiterThread){ "Waiter not expected to be null here"}
        waiterThread!!.value = value
        waiterThread!!.condition.signal()
        waiterThread = null
    }

}






